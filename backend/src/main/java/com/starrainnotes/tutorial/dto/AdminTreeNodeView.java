package com.starrainnotes.tutorial.dto;

import java.util.List;

/**
 * Admin node tree node (04 §10): never includes chapter body markdown.
 * parentId lets the admin UI compute Move Up/Down target siblings.
 */
public record AdminTreeNodeView(
        Long id,
        Long parentId,
        String type,
        String title,
        String slug,
        String publishStatus,
        Integer sortOrder,
        List<AdminTreeNodeView> children) {
}
