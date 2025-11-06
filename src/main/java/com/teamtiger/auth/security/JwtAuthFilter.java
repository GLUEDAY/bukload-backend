// com.teamtiger.auth.security.JwtAuthFilter.java
package com.teamtiger.auth.security;

import com.teamtiger.auth.user.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest http, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String auth = http.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                String subject = jwtUtil.getSubject(token); // subject = loginId
                var user = userRepo.findByLoginId(subject).orElse(null); // ✅ loginId로 조회
                if (user != null) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            user, null,
                            java.util.List.of(new org.springframework.security.core.authority.
                                    SimpleGrantedAuthority("ROLE_" + user.getRole()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ignored) {}
        }
        chain.doFilter(http, res);
    }
}