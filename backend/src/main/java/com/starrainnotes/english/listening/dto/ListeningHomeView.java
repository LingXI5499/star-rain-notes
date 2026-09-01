package com.starrainnotes.english.listening.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ListeningHomeView(
        long total,
        Map<Integer, Long> byLevel,
        Map<String, Long> byCefr,
        java.util.List<ListeningTagRef> topics,
        java.util.List<ListeningTagRef> scenes,
        java.util.List<ListeningTagRef> formats) {
}
