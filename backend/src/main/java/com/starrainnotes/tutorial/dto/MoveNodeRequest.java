package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.Min;

/**
 * POST /api/v1/admin/tutorials/{tutorialId}/nodes/{nodeId}/move request body.
 * targetParentId must be a GROUP of the same tutorial or null;
 * targetIndex is 0-based (clamped to the sibling list bounds).
 */
public record MoveNodeRequest(
        Long targetParentId,
        @Min(0) int targetIndex) {
}
