package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.Min;

/** Moves an item within its current flat sibling list. */
public record MoveIndexRequest(@Min(0) int targetIndex) {
}
