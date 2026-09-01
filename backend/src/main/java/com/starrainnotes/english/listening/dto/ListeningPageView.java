package com.starrainnotes.english.listening.dto;

import java.util.List;
import java.util.Map;

public record ListeningPageView(
        List<ListeningItemSummaryView> items,
        int page,
        int pageSize,
        long total,
        int totalPages,
        ListeningAdminStats stats) {
}
