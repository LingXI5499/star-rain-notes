package com.starrainnotes.english.grammar.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GrammarMoveRequest(@NotNull @Min(0) Integer targetIndex) {
}
