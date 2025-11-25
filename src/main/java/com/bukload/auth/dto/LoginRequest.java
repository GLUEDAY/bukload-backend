// LoginRequest.java
package com.bukload.auth.dto;
import jakarta.validation.constraints.NotBlank;

// auth/dto/LoginRequest.java  (로그인은 아이디+비번)
public record LoginRequest(@NotBlank String loginId, @NotBlank String password) {}


