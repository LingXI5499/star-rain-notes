package com.starrainnotes.english.shared.bundle.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BundleItemMoveRequest(@NotNull @Min(0) Integer targetIndex) {
}
