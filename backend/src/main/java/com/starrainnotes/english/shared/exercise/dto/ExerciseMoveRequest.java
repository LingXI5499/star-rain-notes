package com.starrainnotes.english.shared.exercise.dto;

import jakarta.validation.constraints.Min;

public record ExerciseMoveRequest(@Min(0) Integer targetIndex) {
}
