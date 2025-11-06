package com.teamtiger.auth.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDate;
import java.util.List;

// com.teamtiger.auth.user.User.java
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "users", indexes = {
        @Index(name="uk_email", columnList = "email", unique = true),
        @Index(name="uk_login_id", columnList = "loginId", unique = true)
})
public class User implements UserDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=60)
    private String loginId;            // ← 아이디(별도)

    @Column(nullable=false, unique=true, length=120)
    private String email;

    @Column(nullable=false)
    private String password;           // BCrypt

    @Column(nullable=false, length=30)
    private String role;               // USER/ADMIN

    // 프로필
    @Column(length=60)  private String name;
    @Enumerated(EnumType.STRING) private Gender gender; // MALE/FEMALE/OTHER
    private LocalDate birthDate;
    @Column(length=60)  private String nickname;
    @Column(length=30)  private String preferredTheme;  // e.g. LIGHT/DARK
    @Column(length=120) private String homeLocation;

    // 소셜
    @Column(length=20)  private String provider;   // kakao/naver/google
    @Column(length=120) private String providerId; // 공급자 유저ID

    // refresh 토큰(옵션: Redis로 빼도 됨)
    @Column(length=512) private String refreshToken;

    @Override public List<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
    @Override public String getUsername() { return loginId; } // ← 변경 포인트
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    public enum Gender {
        MALE, FEMALE, OTHER
    }

}
