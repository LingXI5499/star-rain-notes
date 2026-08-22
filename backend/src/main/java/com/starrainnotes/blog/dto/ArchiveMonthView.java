package com.starrainnotes.blog.dto;

/**
 * One month inside an archive year.
 */
public record ArchiveMonthView(
        String month,
        long count) {
}
