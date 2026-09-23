package com.starrainnotes.english.vocabulary.learning.application;

import com.starrainnotes.english.vocabulary.learning.infrastructure.VocabularyStudyRepository;
import com.starrainnotes.english.vocabulary.learning.infrastructure.VocabularyReviewLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
public class VocabularyProgressImportService {
    private final VocabularyStudyRepository studyRepository;
    private final VocabularyReviewLogRepository reviewLogs;

    public VocabularyProgressImportService(VocabularyStudyRepository studyRepository,
                                           VocabularyReviewLogRepository reviewLogs) {
        this.studyRepository = studyRepository;
        this.reviewLogs = reviewLogs;
    }
    @Transactional
    public void importLocal(long accountId, Map<String, Object> payload) {
        Object raw = payload == null ? null : (payload.containsKey("memory") ? payload.get("memory") : payload.get("vocabulary"));
        LocalDateTime now = utcNow();
        if (raw instanceof Map<?, ?> entries) {
            for (Map.Entry<?, ?> entry : entries.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> value) importMemory(accountId, longValue(entry.getKey()), value, now);
            }
        } else if (raw instanceof List<?> entries) {
            for (Object entry : entries) {
                if (entry instanceof Map<?, ?> value) importMemory(accountId, longValue(value.get("wordId")), value, now);
            }
        }
        Object logs = payload == null ? null : payload.get("reviewLog");
        if (logs instanceof List<?> reviews) {
            for (Object item : reviews) if (item instanceof Map<?, ?> review) importReview(accountId, review, now);
        }
    }

    private void importMemory(long accountId, long wordId, Map<?, ?> value, LocalDateTime now) {
        if (wordId <= 0 || !studyRepository.wordExists(wordId)) return;
        int memoryCount = Math.max(0, intValue(value.get("memoryCount")));
        int reviewCount = Math.max(0, intValue(value.get("reviewCount")));
        if (memoryCount == 0 && reviewCount == 0) return;
        LocalDateTime first = parseClientTime(value.get("firstLearnedAt"), now);
        LocalDateTime last = parseClientTime(value.get("lastReviewedAt"), now);
        LocalDateTime next = parseClientTime(value.get("nextReviewAt"), now);
        studyRepository.importMemory(accountId, wordId, memoryCount, reviewCount, first, last, next, now);
    }

    private void importReview(long accountId, Map<?, ?> value, LocalDateTime now) {
        long wordId = longValue(value.get("wordId"));
        String sessionId = String.valueOf(value.get("reviewSessionId"));
        String direction = String.valueOf(value.get("direction"));
        int reviewNumber = Math.max(1, intValue(value.get("reviewNumber")));
        long interval = Math.max(1L, longValue(value.get("intervalSeconds")));
        String timing = String.valueOf(value.get("timingStatus"));
        try { java.util.UUID.fromString(sessionId); } catch (RuntimeException ex) { return; }
        if (wordId <= 0 || !studyRepository.wordExists(wordId) || !("EN_TO_ZH".equals(direction) || "ZH_TO_EN".equals(direction))) return;
        if (!List.of("NEW", "EARLY", "ON_TIME", "OVERDUE").contains(timing)) timing = "NEW";
        LocalDateTime reviewed = parseClientTime(value.get("reviewedAt"), now);
        if (reviewed == null || reviewed.isAfter(now.plusMinutes(5))) reviewed = now;
        LocalDateTime scheduled = parseClientTime(value.get("scheduledAt"), now);
        reviewLogs.importReview(accountId, wordId, sessionId, direction, reviewNumber, scheduled, reviewed, interval, timing);
    }

    private int intValue(Object value) {
        if (value instanceof Number number) return number.intValue();
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException ex) { return 0; }
    }

    private long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); }
        catch (NumberFormatException ex) { return 0L; }
    }

    private LocalDateTime parseClientTime(Object value, LocalDateTime now) {
        if (value == null || String.valueOf(value).isBlank() || "null".equals(String.valueOf(value))) return null;
        try {
            LocalDateTime parsed = java.time.OffsetDateTime.parse(String.valueOf(value)).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            return parsed.isAfter(now.plusMinutes(5)) ? now : parsed;
        } catch (java.time.format.DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
