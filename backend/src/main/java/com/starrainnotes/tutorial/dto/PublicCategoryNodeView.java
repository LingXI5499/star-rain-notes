package com.starrainnotes.tutorial.dto;

import java.util.List;

/**
 * Public category tree node — only branches with published content are kept.
 */
public record PublicCategoryNodeView(
        Long id,
        String name,
        String slug,
        List<PublicCategoryNodeView> children) {
}
