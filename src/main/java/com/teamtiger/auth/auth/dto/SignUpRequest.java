// SignUpRequest.java
package com.teamtiger.auth.auth.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// auth/dto/SignUpRequest.java
public record SignUpRequest(
        @NotBlank String loginId,
        @NotBlank String password,
        @NotBlank String passwordConfirm,
        @NotBlank String name,
        @NotBlank String gender,   // "MALE|FEMALE|OTHER"
        @NotBlank String birthDate, // "yyyy-MM-dd"
        @Email String email
) {}

