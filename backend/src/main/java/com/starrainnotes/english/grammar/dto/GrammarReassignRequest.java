package com.starrainnotes.english.grammar.dto;

import jakarta.validation.constraints.NotNull;

public record GrammarReassignRequest(@NotNull Long targetSectionId) {
}
