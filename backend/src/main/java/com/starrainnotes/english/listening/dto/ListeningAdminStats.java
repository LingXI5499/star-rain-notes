package com.starrainnotes.english.listening.dto;

import java.util.Map;

public record ListeningAdminStats(
        long total,
        long published,
        long draft,
        long withdrawn,
        long missingAudio,
        Map<Integer, Long> byLevel) {
}
