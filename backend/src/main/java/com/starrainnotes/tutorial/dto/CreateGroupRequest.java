package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/admin/tutorials/{tutorialId}/groups request body.
 */
public record CreateGroupRequest(
        @NotBlank @Size(max = 200) String title) {
}
