package com.starrainnotes.english.listening.dto;

/** A time segment for one listening item. */
public record ListeningSegmentView(
        Long id,
        Long itemId,
        Integer startMs,
        Integer endMs,
        String transcriptText,
        String translationText,
        Integer sortOrder,
        String updatedAt) {
}
