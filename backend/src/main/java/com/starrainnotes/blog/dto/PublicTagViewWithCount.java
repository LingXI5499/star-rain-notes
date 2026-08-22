package com.starrainnotes.blog.dto;

/**
 * Public blog tag with published post count (tags with no published post are
 * excluded).
 */
public record PublicTagViewWithCount(
        Long id,
        String name,
        String slug,
        long postCount) {
}
