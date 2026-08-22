package com.starrainnotes.english.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/v1/admin/english request body.
 * currentStage is intentionally absent — V1 keeps FOUNDATION (04 §13).
 */
public record UpdateEnglishRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 500) String subtitle,
        String introduction,
        String roadmapMarkdown) {
}
