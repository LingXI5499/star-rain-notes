package com.starrainnotes.english.shared.bundle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BundleItemRequest(@NotBlank String contentType, @NotNull Long contentId) {
}
