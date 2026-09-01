package com.starrainnotes.english.shared.taxonomy.dto;

import java.util.List;

/**
 * A taxonomy term as exposed to clients. {@code children} is only populated on
 * the meta view so the frontend can render a two-level picker without flatter
 * joins; module-filtering endpoints operate on term ids.
 */
public record TaxonomyTermView(
        Long id,
        Long parentId,
        String dimension,
        String name,
        String slug,
        String description,
        Integer sortOrder,
        Boolean enabled,
        String updatedAt,
        List<TaxonomyTermView> children) {
}
