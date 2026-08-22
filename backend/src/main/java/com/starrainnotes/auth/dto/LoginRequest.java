package com.starrainnotes.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/auth/login request body.
 */
public record LoginRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 72) String password) {
}
