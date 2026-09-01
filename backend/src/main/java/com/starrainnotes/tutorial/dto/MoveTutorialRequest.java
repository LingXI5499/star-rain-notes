package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.Min;

/** Moves a tutorial within its current category's manual order. */
public record MoveTutorialRequest(@Min(0) int targetIndex) {
}
