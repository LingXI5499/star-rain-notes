package com.starrainnotes.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String email,
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 10, max = 72) String newPassword) {
}