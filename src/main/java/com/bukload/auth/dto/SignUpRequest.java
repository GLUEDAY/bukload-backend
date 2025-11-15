package com.bukload.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor   // ★ Jackson 역직렬화에 반드시 필요
public class SignUpRequest {

        @NotBlank
        private String loginId;

        @NotBlank
        private String password;

        @NotBlank
        private String passwordConfirm;

        @NotBlank
        private String name;

        // gender는 정확히 MALE/FEMALE/OTHER만 허용
        @NotBlank
        @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "gender must be MALE, FEMALE, or OTHER")
        private String gender;

        // 날짜 형식도 yyyy-MM-dd만 허용
        @NotBlank
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "birthDate must be yyyy-MM-dd format")
        private String birthDate;

        @Email
        private String email;
}
