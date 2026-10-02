package com.starrainnotes.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmEmailDTO(@NotBlank @Pattern(regexp = "[0-9]{6}") String verificationCode) {
}
