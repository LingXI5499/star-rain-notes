package com.starrainnotes.blog.dto;

import java.util.List;

/**
 * Calendar for one month (published posts grouped by site-tz day).
 */
public record CalendarView(
        String month,
        List<CalendarDayView> days) {
}
