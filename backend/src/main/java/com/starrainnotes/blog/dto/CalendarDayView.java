package com.starrainnotes.blog.dto;

/**
 * One calendar day with published post count (site timezone).
 */
public record CalendarDayView(
        String date,
        long count) {
}
