package com.starrainnotes.english.listening.dto;

import java.util.List;

/** Listening material detail (admin/public). Includes tags, segments and paired reading. */
public record ListeningItemView(
        Long id,
        String title,
        String slug,
        String summary,
        String transcriptMarkdown,
        String cefrLevel,
        Integer listeningLevel,
        Long audioMediaId,
        String audioUrl,
        Long coverMediaId,
        String coverUrl,
        Integer durationSeconds,
        String sourceName,
        String sourceUrl,
        String copyrightNote,
        String publishStatus,
        Integer sortOrder,
        String publishedAt,
        String updatedAt,
        List<ListeningTagRef> tags,
        List<ListeningSegmentView> segments,
        List<ReadingPairRef> readingPairs,
        ListeningLinkView previous,
        ListeningLinkView next) {

    public ListeningItemView withNav(ListeningLinkView prev, ListeningLinkView nxt) {
        return new ListeningItemView(id, title, slug, summary, transcriptMarkdown, cefrLevel, listeningLevel,
                audioMediaId, audioUrl, coverMediaId, coverUrl, durationSeconds, sourceName, sourceUrl,
                copyrightNote, publishStatus, sortOrder, publishedAt, updatedAt, tags, segments, readingPairs,
                prev, nxt);
    }

    public ListeningItemView withReadingPairs(List<ReadingPairRef> pairs) {
        return new ListeningItemView(id, title, slug, summary, transcriptMarkdown, cefrLevel, listeningLevel,
                audioMediaId, audioUrl, coverMediaId, coverUrl, durationSeconds, sourceName, sourceUrl,
                copyrightNote, publishStatus, sortOrder, publishedAt, updatedAt, tags, segments, pairs,
                previous, next);
    }
}
