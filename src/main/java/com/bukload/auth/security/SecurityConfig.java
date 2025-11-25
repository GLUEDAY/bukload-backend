package com.bukload.auth.security;

import com.bukload.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserRepository userRepo;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // 🔥 WebConfig(CORS 설정)과 연동
                .cors(cors -> cors.configure(http))

                // 🔥 CSRF 비활성화 (JWT 환경)
                .csrf(csrf -> csrf.disable())

                // 🔥 URL 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // OPTIONS 프리플라이트 요청 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 기존 SecurityConfig에서 허용했던 endpoint들
                        .requestMatchers(
                                "/auth/signup",
                                "/auth/login",
                                "/auth/token/refresh",
                                "/auth/*/login",
                                "/auth/*/callback",
                                "/h2-console/**",
                                "/api/transit/**",
                                "/api/places/**"
                        ).permitAll()

                        // 새 SecurityConfigForFront에서 포함된 /auth/**
                        .requestMatchers("/auth/**").permitAll()

                        // 나머지는 전부 인증 필요
                        .anyRequest().authenticated()
                )

                // 🔥 JWT 환경 → 세션 사용 안 함
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // H2 콘솔 프레임 옵션 비활성화
                .headers(h -> h.frameOptions(f -> f.disable()));

        // 🔥 JWT 필터는 UsernamePasswordAuthenticationFilter 앞에서 실행
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // 로그인은 loginId 기준
        return username -> userRepo.findByLoginId(username)
                .orElseThrow(() -> new UsernameNotFoundException("회원 없음"));
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
