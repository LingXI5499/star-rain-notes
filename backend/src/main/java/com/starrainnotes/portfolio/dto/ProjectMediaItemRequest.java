package com.starrainnotes.portfolio.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Gallery item in create/update project payloads.
 * id null = new row; existing ids not listed are removed on replace-sync.
 */
public record ProjectMediaItemRequest(
        Long id,
        @NotNull Long mediaAssetId,
        @Size(max = 200) String title,
        @Size(max = 1000) String description,
        @Size(max = 300) String altText,
        @Pattern(regexp = "^(DESKTOP|MOBILE|TABLET)$", message = "deviceType must be DESKTOP, MOBILE or TABLET")
        String deviceType,
        Integer sortOrder) {
}
