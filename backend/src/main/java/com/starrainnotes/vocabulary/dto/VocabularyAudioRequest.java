package com.starrainnotes.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VocabularyAudioRequest(
        @NotNull Long mediaAssetId,
        @NotBlank @Pattern(regexp = "US|UK") String accent,
        @Pattern(regexp = "UPLOADED|LICENSED_API|OTHER") String provider,
        @Size(max = 500) String sourceUrl,
        @NotBlank @Size(max = 500) String licenseNote,
        boolean primary) {
}
