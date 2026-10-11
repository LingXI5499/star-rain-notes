package com.starrainnotes.blog.api.event;

/** Topic page change published after the blog transaction commits. */
public record BlogTopicChangedEvent(Long topicId, String previousSlug, String slug, boolean visible) {
}
