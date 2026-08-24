package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.Min;

/**
 * Moves a category to a parent and 0-based sibling index.  A null parent
 * keeps the category at the root level.
 */
public record MoveCategoryRequest(
        Long targetParentId,
        @Min(0) int targetIndex) {
}
