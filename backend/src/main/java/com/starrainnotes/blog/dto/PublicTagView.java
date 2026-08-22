package com.starrainnotes.blog.dto;

/**
 * Tag reference inside public post views.
 */
public record PublicTagView(
        Long id,
        String name,
        String slug) {
}
