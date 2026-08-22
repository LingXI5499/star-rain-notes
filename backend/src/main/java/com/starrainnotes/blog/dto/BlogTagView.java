package com.starrainnotes.blog.dto;

/**
 * Blog tag view (admin list + post tags).
 */
public record BlogTagView(
        Long id,
        String name,
        String slug) {
}
