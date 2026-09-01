package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.Min;

/** Moves a root knowledge system to a 0-based sibling index. */
public record MoveCategoryRequest(@Min(0) int targetIndex) {
}
