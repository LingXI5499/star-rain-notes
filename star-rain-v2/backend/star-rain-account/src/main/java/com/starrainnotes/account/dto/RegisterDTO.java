package com.starrainnotes.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterDTO(@NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,50}") String username,
        @NotBlank @Email @Size(max = 128) String email,
        @NotBlank @Pattern(regexp = "[0-9]{6}") String verificationCode,
        @NotBlank String password,
        @NotBlank String confirmPassword) {
}
