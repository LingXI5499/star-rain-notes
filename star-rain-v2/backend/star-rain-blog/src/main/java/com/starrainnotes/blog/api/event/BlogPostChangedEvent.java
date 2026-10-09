package com.starrainnotes.blog.api.event;

/** A published post was edited; derived search and SEO data must be refreshed. */
public record BlogPostChangedEvent(Long postId, String previousSlug, String slug) {
}
