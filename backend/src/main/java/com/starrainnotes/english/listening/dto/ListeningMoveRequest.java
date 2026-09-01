package com.starrainnotes.english.listening.dto;

import jakarta.validation.constraints.Min;

public record ListeningMoveRequest(@Min(0) Integer targetIndex) {
}
