package com.starrainnotes.english.shared.bundle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BundleRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 150) String slug,
        @Size(max = 1000) String summary,
        @Size(max = 2) String primaryCefr,
        Long coverMediaId,
        Integer sortOrder) {
}
