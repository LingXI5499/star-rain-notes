package com.starrainnotes.blog.api.dto;

import java.time.LocalDateTime;

/** Public topic metadata used by SEO without exposing blog persistence types. */
public record BlogTopicSummary(Long id, String slug, String name, String description,
                               LocalDateTime updatedAt) {
}
