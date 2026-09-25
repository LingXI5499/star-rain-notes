package com.starrainnotes.english.writing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WritingMoveRequest(@NotNull @Min(0) Integer targetIndex) {
}
