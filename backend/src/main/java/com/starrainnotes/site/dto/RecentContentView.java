package com.starrainnotes.site.dto;

/**
 * One row of the dashboard recent-content list (type TUTORIAL|CHAPTER|BLOG|PORTFOLIO).
 */
public record RecentContentView(
        String type,
        Long id,
        String title,
        String publishStatus,
        String updatedAt) {
}
