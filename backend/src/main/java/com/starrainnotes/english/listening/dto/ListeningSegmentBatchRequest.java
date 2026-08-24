package com.starrainnotes.english.listening.dto;

import java.util.List;

/** Batch save for a listening item's time segments (transactional). */
public record ListeningSegmentBatchRequest(List<ListeningSegmentRequest> segments) {
}
