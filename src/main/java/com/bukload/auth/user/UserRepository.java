// com.teamtiger.auth.user.UserRepository.java
package com.bukload.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// com.teamtiger.auth.user.UserRepository.java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);  // ✅
    Optional<User> findByEmail(String email);
    boolean existsByLoginId(String loginId);       // ✅
    boolean existsByEmail(String email);
}