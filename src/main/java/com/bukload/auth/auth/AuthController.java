package com.bukload.auth.auth;

import com.bukload.auth.dto.LoginRequest;
import com.bukload.auth.dto.RefreshRequest;
import com.bukload.auth.dto.SignUpRequest;
import com.bukload.auth.dto.TokenPairResponse;
import com.bukload.auth.security.JwtUtil;
import com.bukload.auth.user.User;
import com.bukload.auth.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

// auth/AuthController.java
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwt;
    private final AuthenticationManager authManager;
    private final UserRepository userRepo;

    @PostMapping("/signup")
    public TokenPairResponse signUp(@Valid @RequestBody SignUpRequest req) {
        User u = authService.signUp(req);
        System.out.println("LOADED DTO = " + req.getClass());

        return authService.issueTokens(u);
    }

    @PostMapping("/login")
    public TokenPairResponse login(@Valid @RequestBody LoginRequest req) {
        var token = new UsernamePasswordAuthenticationToken(req.loginId(), req.password());
        authManager.authenticate(token); // 검증 실패시 예외
        User u = userRepo.findByLoginId(req.loginId()).orElseThrow();
        return authService.issueTokens(u);
    }

    @PostMapping("/token/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    // 소셜 시작: 공급자 인증 URL로 리다이렉트(간단 버전)
    @GetMapping("/{provider}/login")
    public void socialLoginStart(@PathVariable String provider, HttpServletResponse res) throws IOException {
        // 실제로는 각 공급자 인가 URL 구성 필요. 여기선 예시로 콜백으로 보냄.
        res.sendRedirect("/auth/" + provider + "/callback?code=dummy-code");
    }

    // 소셜 콜백
    @GetMapping("/{provider}/callback")
    public TokenPairResponse socialCallback(@PathVariable String provider, @RequestParam String code) {
        return authService.socialLogin(provider, code);
    }
}
