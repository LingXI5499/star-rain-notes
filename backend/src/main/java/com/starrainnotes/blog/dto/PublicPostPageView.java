package com.starrainnotes.blog.dto;

import java.util.List;

/**
 * Public blog timeline page.
 */
public record PublicPostPageView(
        List<PublicPostSummaryView> items,
        int page,
        int pageSize,
        long total,
        int totalPages) {
}
