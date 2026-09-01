package com.starrainnotes.english.reading.dto;

import jakarta.validation.constraints.Min;

public record ReadingExerciseMoveRequest(@Min(0) Integer targetIndex) {
}
