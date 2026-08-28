package com.starrainnotes.english.listening.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Batch save for a listening item's time segments (transactional). */
public record ListeningSegmentBatchRequest(@NotNull List<@Valid ListeningSegmentRequest> segments) {
}
