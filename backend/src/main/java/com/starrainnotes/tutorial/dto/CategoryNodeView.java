package com.starrainnotes.tutorial.dto;

import java.util.List;

/**
 * Admin category tree node (nested).
 */
public record CategoryNodeView(
        Long id,
        String name,
        String slug,
        Integer sortOrder,
        Long parentId,
        List<CategoryNodeView> children) {
}
