package com.teamtiger.auth.auth;
import com.teamtiger.auth.user.User;
import com.teamtiger.auth.auth.dto.SignUpRequest;
import com.teamtiger.auth.auth.dto.TokenPairResponse;
import com.teamtiger.auth.security.JwtUtil;
import com.teamtiger.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

// auth/AuthService.java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public User signUp(SignUpRequest req) {
        if (!req.password().equals(req.passwordConfirm()))
            throw new IllegalArgumentException("비밀번호 확인 불일치");
        if (userRepo.existsByLoginId(req.loginId()))
            throw new IllegalArgumentException("이미 사용 중인 아이디");
        if (userRepo.existsByEmail(req.email()))
            throw new IllegalArgumentException("이미 가입된 이메일");

        User u = User.builder()
                .loginId(req.loginId())
                .email(req.email())
                .password(encoder.encode(req.password()))
                .role("USER")
                .name(req.name())
                .gender(User.Gender.valueOf(req.gender()))
                .birthDate(LocalDate.parse(req.birthDate()))
                .build();
        // 최초 refresh 발급 (선택)
        String refresh = jwt.createRefresh(u.getLoginId());
        u.setRefreshToken(refresh);
        return userRepo.save(u);
    }

    public TokenPairResponse issueTokens(User user) {
        String access = jwt.createAccess(user.getLoginId());
        String refresh = jwt.createRefresh(user.getLoginId());
        user.setRefreshToken(refresh);
        userRepo.save(user);
        return new TokenPairResponse(access, refresh);
    }

    public TokenPairResponse refresh(String refreshToken) {
        String typ = jwt.getType(refreshToken);
        if (!"refresh".equals(typ)) throw new IllegalArgumentException("리프레시 토큰 아님");
        String loginId = jwt.getSubject(refreshToken);
        User u = userRepo.findByLoginId(loginId).orElseThrow();
        if (!refreshToken.equals(u.getRefreshToken()))
            throw new IllegalArgumentException("저장된 토큰과 불일치");
        return issueTokens(u); // 새 access/refresh 교체 발급
    }

    // --- 소셜 ---
    public TokenPairResponse socialLogin(String provider, String code) {
        // 1) provider 토큰 교환  2) 유저정보 조회  3) 회원 존재여부 확인/생성  4) 토큰 발급
        // 여기서는 최소 동작 샘플 (실제 HTTP 호출/매핑은 SocialClient로 분리 권장)
        String providerId = provider + "_dummy_" + code; // 예시
        String email = providerId + "@example.com";
        User user = userRepo.findByEmail(email).orElseGet(() -> {
            User u = User.builder()
                    .loginId(provider + "_" + providerId)
                    .email(email)
                    .password(encoder.encode(UUID.randomUUID().toString()))
                    .role("USER")
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            return userRepo.save(u);
        });
        return issueTokens(user);
    }
}
