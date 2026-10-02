package com.starrainnotes.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationCodeRequestDTO(@NotBlank @Email @Size(max = 128) String email) {
}
