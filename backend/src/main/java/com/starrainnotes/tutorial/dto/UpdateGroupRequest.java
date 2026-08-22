package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/v1/admin/tutorials/{tutorialId}/groups/{groupId} request body
 * (rename only; repositioning goes through POST /nodes/{nodeId}/move).
 */
public record UpdateGroupRequest(
        @NotBlank @Size(max = 200) String title) {
}
