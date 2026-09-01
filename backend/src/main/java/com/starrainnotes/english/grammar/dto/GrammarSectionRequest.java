package com.starrainnotes.english.grammar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GrammarSectionRequest(@NotBlank @Size(max = 200) String title) {
}
