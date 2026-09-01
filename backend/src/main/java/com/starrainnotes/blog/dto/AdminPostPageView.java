package com.starrainnotes.blog.dto;

import java.util.List;

/**
 * Admin blog post page (04 §3 envelope).
 */
public record AdminPostPageView(
        List<AdminPostSummaryView> items,
        int page,
        int pageSize,
        long total,
        int totalPages) {
}
