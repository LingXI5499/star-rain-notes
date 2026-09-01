package com.starrainnotes.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivationConfirmRequest(
        @NotBlank String verificationCode,
        @NotBlank @Size(min = 10, max = 72) String password) {
}