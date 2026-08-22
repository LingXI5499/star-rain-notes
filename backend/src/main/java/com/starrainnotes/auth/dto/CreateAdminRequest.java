package com.starrainnotes.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/setup/admin request body.
 * The setup token travels in the X-Setup-Token header (constant-time matched).
 */
public record CreateAdminRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
