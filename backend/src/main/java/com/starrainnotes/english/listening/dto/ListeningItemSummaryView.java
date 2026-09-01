package com.starrainnotes.english.listening.dto;

import java.util.List;

public record ListeningItemSummaryView(
        Long id,
        String title,
        String slug,
        String summary,
        String coverUrl,
        String cefrLevel,
        Integer listeningLevel,
        Integer durationSeconds,
        String publishStatus,
        long exerciseCount,
        long segmentCount,
        String updatedAt,
        List<ListeningTagRef> tags) {

    public ListeningItemSummaryView withTags(List<ListeningTagRef> tags) {
        return new ListeningItemSummaryView(id, title, slug, summary, coverUrl, cefrLevel, listeningLevel,
                durationSeconds, publishStatus, exerciseCount, segmentCount, updatedAt, tags);
    }

    public ListeningItemSummaryView withCounts(long exerciseCount, long segmentCount) {
        return new ListeningItemSummaryView(id, title, slug, summary, coverUrl, cefrLevel, listeningLevel,
                durationSeconds, publishStatus, exerciseCount, segmentCount, updatedAt, tags);
    }
}
