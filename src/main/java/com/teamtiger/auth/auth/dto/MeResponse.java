// MeResponse.java
package com.teamtiger.auth.auth.dto;

public record MeResponse(
        Long id, String loginId, String email, String role,
        String name, String gender, String birthDate,
        String nickname, String preferredTheme, String homeLocation
) {}