package com.starrainnotes.blog.dto;

/**
 * Previous/next blog post on the global publication timeline.
 */
public record PrevNextPostView(
        Long postId,
        String slug,
        String title) {
}
