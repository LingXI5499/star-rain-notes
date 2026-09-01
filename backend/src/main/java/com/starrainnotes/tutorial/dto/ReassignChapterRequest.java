package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.NotNull;

/** Explicitly reassigns a chapter to another group in the same tutorial. */
public record ReassignChapterRequest(@NotNull Long targetGroupId) {
}
