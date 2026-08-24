package com.starrainnotes.english.shared.taxonomy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload for a taxonomy term. Validation is completed in the
 * service so that dimension legality, parent depth/cycle and slug uniqueness
 * are enforced server-side.
 */
public record TaxonomyRequest(
        @NotBlank @Size(max = 20) String dimension,
        Long parentId,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String slug,
        @Size(max = 500) String description,
        @Min(1) Integer sortOrder,
        Boolean enabled) {
}
