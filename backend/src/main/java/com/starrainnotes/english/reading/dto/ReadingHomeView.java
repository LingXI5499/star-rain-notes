package com.starrainnotes.english.reading.dto;

import java.util.List;
import java.util.Map;

/**
 * Public reading home summary (方案 §十): route levels, per-CEFR counts and the
 * filter dictionaries. Deliberately small — no article body is carried here.
 */
public record ReadingHomeView(
        long total,
        Map<Integer, Long> byLevel,
        Map<String, Long> byCefr,
        List<ReadingTagRef> topics,
        List<ReadingTagRef> genres) {
}
