package com.starrainnotes.english.reading.dto;

import jakarta.validation.constraints.Min;

@Deprecated
public record ReadingExerciseMoveRequest(@Min(0) Integer targetIndex) {
}
