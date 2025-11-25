package com.bukload.auth.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.List;

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
    private String loginId;

    @Column(nullable=false, unique=true, length=120)
    private String email;

    @Column(nullable=false)
    private String password;

    @Column(nullable=false, length=30)
    private String role;

    // =======================
    //  ⭐ 프로필
    // =======================
    @Column(length=60)
    private String name;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate birthDate;

    @Column(length=60)
    private String nickname;

    @Column(length=30)
    private String preferredTheme;

    @Column(length=120)
    private String homeLocation;

    // =======================
    //  ⭐ 포인트 컬럼 추가!
    // =======================
    @Builder.Default
    @Column(nullable = false)
    private int point = 0;

    public void addPoint(int amount) {
        this.point += amount;
    }

    // =======================
    //  소셜
    // =======================
    @Column(length=20)
    private String provider;

    @Column(length=120)
    private String providerId;

    @Column(length=512)
    private String refreshToken;

    // =======================
    //  Spring Security
    // =======================
    @Override
    public List<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public String getUsername() { return loginId; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    public enum Gender {
        MALE, FEMALE, OTHER
    }
}
