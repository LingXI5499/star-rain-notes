package com.starrainnotes.blog.dto;

import java.util.List;

/**
 * Archive grouped by year DESC / month DESC.
 */
public record ArchiveYearView(
        int year,
        List<ArchiveMonthView> months) {
}
