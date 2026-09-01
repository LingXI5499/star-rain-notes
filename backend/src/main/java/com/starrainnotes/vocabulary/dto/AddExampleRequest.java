package com.starrainnotes.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * User-added example sentence (translation optional).
 */
public record AddExampleRequest(
        @NotBlank @Size(max = 1000) String sentence,
        @Size(max = 1000) String translation) {
}
