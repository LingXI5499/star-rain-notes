package com.starrainnotes.account.english.vocabulary;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.vocabulary.service.VocabularyService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VocabularyStudyService {

    private static final long[] REVIEW_INTERVAL_SECONDS = {
            300L, 1_800L, 43_200L, 86_400L, 172_800L,
            345_600L, 604_800L, 1_296_000L, 2_592_000L, 5_184_000L
    };
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final JdbcTemplate jdbc;
    private final VocabularyService vocabulary;
    private final SiteSettingsTimezone timezone;

    public VocabularyStudyService(JdbcTemplate jdbc, VocabularyService vocabulary, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.vocabulary = vocabulary;
        this.timezone = timezone;
    }

    public VocabularyStudySettingsView settings(long accountId) {
        ensureSettings(accountId);
        return jdbc.queryForObject("""
                SELECT show_english,show_chinese,review_direction,daily_new_limit,daily_review_limit
                FROM account_vocabulary_study_setting WHERE account_id=?
                """, (rs, rowNum) -> new VocabularyStudySettingsView(
                rs.getBoolean("show_english"), rs.getBoolean("show_chinese"),
                rs.getString("review_direction"), rs.getInt("daily_new_limit"),
                rs.getInt("daily_review_limit")), accountId);
    }

    @Transactional
    public VocabularyStudySettingsView updateSettings(long accountId, VocabularyStudySettingsRequest request) {
        if (!request.showEnglish() && !request.showChinese()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VOCABULARY_LANGUAGE_REQUIRED",
                    "A language must remain visible", "English and Chinese cannot both be hidden.");
        }
        jdbc.update("""
                INSERT INTO account_vocabulary_study_setting
                    (account_id,show_english,show_chinese,review_direction,daily_new_limit,daily_review_limit)
                VALUES (?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE show_english=VALUES(show_english),show_chinese=VALUES(show_chinese),
                    review_direction=VALUES(review_direction),daily_new_limit=VALUES(daily_new_limit),
                    daily_review_limit=VALUES(daily_review_limit)
                """, accountId, request.showEnglish(), request.showChinese(), request.reviewDirection(),
                request.dailyNewLimit(), request.dailyReviewLimit());
        return settings(accountId);
    }

    public List<VocabularyMemoryView> memories(long accountId, List<Long> wordIds) {
        if (wordIds == null || wordIds.isEmpty()) return List.of();
        List<Long> ids = wordIds.stream().filter(java.util.Objects::nonNull).distinct().limit(500).toList();
        if (ids.isEmpty()) return List.of();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        return jdbc.query("""
                SELECT w.id AS word_id,COALESCE(m.memory_count,0) AS memory_count,
                       COALESCE(m.review_step,0) AS review_step,COALESCE(m.review_count,0) AS review_count,
                       m.first_learned_at,m.last_reviewed_at,m.next_review_at,m.learning_status,p.display_mode
                FROM vocabulary_word w
                LEFT JOIN account_vocabulary_memory m ON m.word_id=w.id AND m.account_id=?
                LEFT JOIN account_vocabulary_card_preference p ON p.word_id=w.id AND p.account_id=?
                WHERE w.id IN (%s) ORDER BY w.id
                """.formatted(placeholders), (rs, rowNum) -> memoryView(rs), prepend(accountId, ids));
    }

    @Transactional
    public VocabularyMemoryView start(long accountId, long wordId) {
        vocabulary.getWord(wordId);
        LocalDateTime now = utcNow();
        jdbc.update("""
                INSERT INTO account_vocabulary_memory
                    (account_id,word_id,memory_count,review_step,review_count,first_learned_at,
                     last_reviewed_at,next_review_at,learning_status,last_memory_at)
                VALUES (?,?,0,0,0,?,NULL,?,'ACTIVE',NULL)
                ON DUPLICATE KEY UPDATE learning_status='ACTIVE',
                    first_learned_at=COALESCE(first_learned_at,VALUES(first_learned_at)),
                    next_review_at=COALESCE(next_review_at,VALUES(next_review_at))
                """, accountId, wordId, now, now);
        return memory(accountId, wordId);
    }

    public VocabularyQueueView queue(long accountId, Long themeId) {
        VocabularyStudySettingsView setting = settings(accountId);
        LocalDateTime now = utcNow();
        LocalDateTime todayStart = LocalDate.now(siteZone()).atStartOfDay(siteZone())
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        int dueCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM account_vocabulary_memory
                WHERE account_id=? AND learning_status='ACTIVE' AND next_review_at<=?
                """, Integer.class, accountId, now);
        List<Long> dueIds = jdbc.queryForList("""
                SELECT word_id FROM account_vocabulary_memory
                WHERE account_id=? AND learning_status='ACTIVE' AND next_review_at<=?
                ORDER BY next_review_at,word_id LIMIT ?
                """, Long.class, accountId, now, setting.dailyReviewLimit());

        List<Long> newIds = List.of();
        int introducedToday = jdbc.queryForObject("""
                SELECT COUNT(*) FROM account_vocabulary_memory
                WHERE account_id=? AND first_learned_at>=?
                """, Integer.class, accountId, todayStart);
        int remainingNewLimit = Math.max(0, setting.dailyNewLimit() - introducedToday);
        if (themeId != null && remainingNewLimit > 0) {
            newIds = jdbc.queryForList("""
                    SELECT w.id FROM vocabulary_word w
                    WHERE w.theme_id=? AND NOT EXISTS (
                        SELECT 1 FROM account_vocabulary_memory m
                        WHERE m.account_id=? AND m.word_id=w.id
                    )
                    ORDER BY w.sort_order,w.id LIMIT ?
                    """, Long.class, themeId, accountId, remainingNewLimit);
        }
        List<Long> orderedIds = new ArrayList<>(dueIds);
        for (Long id : newIds) if (!orderedIds.contains(id)) orderedIds.add(id);
        List<VocabularyWordView> words = vocabulary.getWords(orderedIds);
        Map<Long, VocabularyMemoryView> states = new HashMap<>();
        for (VocabularyMemoryView state : memories(accountId, orderedIds)) states.put(state.wordId(), state);
        Map<Long, VocabularyWordView> wordById = new LinkedHashMap<>();
        for (VocabularyWordView word : words) wordById.put(word.id(), word);
        long day = LocalDate.now(siteZone()).toEpochDay();
        List<VocabularyStudyCardView> items = new ArrayList<>();
        for (Long id : orderedIds) {
            VocabularyWordView word = wordById.get(id);
            if (word == null) continue;
            boolean isNew = !states.containsKey(id) || "NEW".equals(states.get(id).learningStatus());
            VocabularyMemoryView state = states.getOrDefault(id,
                    new VocabularyMemoryView(id, 0, 0, 0, null, null, null, "NEW", null));
            String direction = setting.reviewDirection();
            if ("MIXED".equals(direction)) direction = ((id + day) & 1L) == 0 ? "EN_TO_ZH" : "ZH_TO_EN";
            items.add(new VocabularyStudyCardView(word, state, direction, isNew));
        }
        return new VocabularyQueueView(items, dueCount, newIds.size(), format(now));
    }

    @Transactional
    public VocabularyReviewResultView completeReview(long accountId, long wordId, VocabularyReviewRequest request) {
        vocabulary.getWord(wordId);
        List<Map<String, Object>> duplicate = jdbc.queryForList("""
                SELECT word_id,review_number,interval_seconds,timing_status FROM account_vocabulary_review_log
                WHERE account_id=? AND review_session_id=? LIMIT 1
                """, accountId, request.reviewSessionId());
        if (!duplicate.isEmpty()) {
            Map<String, Object> row = duplicate.getFirst();
            requireSameReviewWord(wordId, row);
            return new VocabularyReviewResultView(memory(accountId, wordId), number(row, "review_number"),
                    longNumber(row, "interval_seconds"), String.valueOf(row.get("timing_status")), true);
        }

        LocalDateTime now = utcNow();
        List<Map<String, Object>> locked = jdbc.queryForList("""
                SELECT memory_count,review_count,next_review_at FROM account_vocabulary_memory
                WHERE account_id=? AND word_id=? FOR UPDATE
                """, accountId, wordId);
        if (locked.isEmpty()) {
            jdbc.update("""
                    INSERT INTO account_vocabulary_memory
                    (account_id,word_id,memory_count,review_step,review_count,first_learned_at,next_review_at,learning_status)
                    VALUES (?,?,0,0,0,?,NULL,'ACTIVE')
                    """, accountId, wordId, now);
            locked = jdbc.queryForList("""
                    SELECT memory_count,review_count,next_review_at FROM account_vocabulary_memory
                    WHERE account_id=? AND word_id=? FOR UPDATE
                    """, accountId, wordId);
        }
        Map<String, Object> old = locked.getFirst();
        int reviewNumber = number(old, "review_count") + 1;
        long intervalSeconds = intervalSeconds(reviewNumber);
        LocalDateTime scheduledAt = dateTime(old.get("next_review_at"));
        String timing = timing(scheduledAt, now);
        LocalDateTime next = now.plusSeconds(intervalSeconds);

        int inserted = jdbc.update("""
                INSERT IGNORE INTO account_vocabulary_review_log
                (account_id,word_id,review_session_id,direction,review_number,scheduled_at,reviewed_at,
                 interval_seconds,timing_status)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, accountId, wordId, request.reviewSessionId(), request.direction(), reviewNumber,
                scheduledAt, now, intervalSeconds, timing);
        if (inserted == 0) {
            Map<String, Object> row = jdbc.queryForMap("""
                    SELECT word_id,review_number,interval_seconds,timing_status
                    FROM account_vocabulary_review_log
                    WHERE account_id=? AND review_session_id=?
                    """, accountId, request.reviewSessionId());
            requireSameReviewWord(wordId, row);
            return new VocabularyReviewResultView(memory(accountId, wordId), number(row, "review_number"),
                    longNumber(row, "interval_seconds"), String.valueOf(row.get("timing_status")), true);
        }
        jdbc.update("""
                UPDATE account_vocabulary_memory SET memory_count=memory_count+1,
                    review_step=?,review_count=?,first_learned_at=COALESCE(first_learned_at,?),
                    last_reviewed_at=?,last_memory_at=?,next_review_at=?,learning_status='ACTIVE'
                WHERE account_id=? AND word_id=?
                """, Math.min(reviewNumber, 10), reviewNumber, now, now, now, next, accountId, wordId);
        return new VocabularyReviewResultView(memory(accountId, wordId), reviewNumber,
                intervalSeconds, timing, false);
    }

    @Transactional
    public void reset(long accountId, long wordId) {
        vocabulary.getWord(wordId);
        jdbc.update("DELETE FROM account_vocabulary_memory WHERE account_id=? AND word_id=?", accountId, wordId);
    }

    @Transactional
    public VocabularyMemoryView setDisplay(long accountId, long wordId, VocabularyDisplayRequest request) {
        vocabulary.getWord(wordId);
        jdbc.update("""
                INSERT INTO account_vocabulary_card_preference(account_id,word_id,display_mode)
                VALUES (?,?,?) ON DUPLICATE KEY UPDATE display_mode=VALUES(display_mode)
                """, accountId, wordId, request.displayMode());
        return optionalMemory(accountId, wordId, request.displayMode());
    }

    @Transactional
    public void clearDisplay(long accountId, long wordId) {
        jdbc.update("DELETE FROM account_vocabulary_card_preference WHERE account_id=? AND word_id=?", accountId, wordId);
    }

    public VocabularyProgressView progress(long accountId) {
        LocalDateTime now = utcNow();
        LocalDateTime start = LocalDate.now(siteZone()).atStartOfDay(siteZone())
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        long active = count("SELECT COUNT(*) FROM account_vocabulary_memory WHERE account_id=? AND learning_status='ACTIVE'", accountId);
        long due = jdbc.queryForObject("SELECT COUNT(*) FROM account_vocabulary_memory WHERE account_id=? AND learning_status='ACTIVE' AND next_review_at<=?", Long.class, accountId, now);
        long today = jdbc.queryForObject("SELECT COUNT(*) FROM account_vocabulary_review_log WHERE account_id=? AND reviewed_at>=?", Long.class, accountId, start);
        long reviews = count("SELECT COUNT(*) FROM account_vocabulary_review_log WHERE account_id=?", accountId);
        long memoryCount = count("SELECT COALESCE(SUM(memory_count),0) FROM account_vocabulary_memory WHERE account_id=?", accountId);
        List<VocabularyReviewHistoryView> recent = jdbc.query("""
                SELECT l.id,l.word_id,w.word,l.review_number,l.direction,l.scheduled_at,l.reviewed_at,
                       l.interval_seconds,l.timing_status
                FROM account_vocabulary_review_log l JOIN vocabulary_word w ON w.id=l.word_id
                WHERE l.account_id=? ORDER BY l.reviewed_at DESC,l.id DESC LIMIT 100
                """, (rs, rowNum) -> new VocabularyReviewHistoryView(rs.getLong("id"), rs.getLong("word_id"),
                rs.getString("word"), rs.getInt("review_number"), rs.getString("direction"),
                format(rs.getObject("scheduled_at", LocalDateTime.class)),
                format(rs.getObject("reviewed_at", LocalDateTime.class)), rs.getLong("interval_seconds"),
                rs.getString("timing_status")), accountId);
        return new VocabularyProgressView(active, due, today, reviews, memoryCount, recent);
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
        if (wordId <= 0 || !wordExists(wordId)) return;
        int memoryCount = Math.max(0, intValue(value.get("memoryCount")));
        int reviewCount = Math.max(0, intValue(value.get("reviewCount")));
        if (memoryCount == 0 && reviewCount == 0) return;
        LocalDateTime first = parseClientTime(value.get("firstLearnedAt"), now);
        LocalDateTime last = parseClientTime(value.get("lastReviewedAt"), now);
        LocalDateTime next = parseClientTime(value.get("nextReviewAt"), now);
        jdbc.update("""
                INSERT INTO account_vocabulary_memory
                (account_id,word_id,memory_count,review_step,review_count,first_learned_at,
                 last_reviewed_at,next_review_at,learning_status,last_memory_at)
                VALUES (?,?,?,?,?,?,?,?, 'ACTIVE',?)
                ON DUPLICATE KEY UPDATE memory_count=GREATEST(memory_count,VALUES(memory_count)),
                    review_step=GREATEST(review_step,VALUES(review_step)),
                    review_count=GREATEST(review_count,VALUES(review_count)),learning_status='ACTIVE',
                    first_learned_at=COALESCE(first_learned_at,VALUES(first_learned_at)),
                    last_reviewed_at=CASE WHEN VALUES(last_reviewed_at) IS NULL THEN last_reviewed_at
                        WHEN last_reviewed_at IS NULL OR VALUES(last_reviewed_at)>last_reviewed_at
                        THEN VALUES(last_reviewed_at) ELSE last_reviewed_at END,
                    next_review_at=COALESCE(next_review_at,VALUES(next_review_at)),
                    last_memory_at=CASE WHEN VALUES(last_memory_at) IS NULL THEN last_memory_at
                        WHEN last_memory_at IS NULL OR VALUES(last_memory_at)>last_memory_at
                        THEN VALUES(last_memory_at) ELSE last_memory_at END
                """, accountId, wordId, memoryCount, Math.min(reviewCount, 10), reviewCount,
                first == null ? now : first, last, next == null ? now : next, last);
    }

    private void importReview(long accountId, Map<?, ?> value, LocalDateTime now) {
        long wordId = longValue(value.get("wordId"));
        String sessionId = String.valueOf(value.get("reviewSessionId"));
        String direction = String.valueOf(value.get("direction"));
        int reviewNumber = Math.max(1, intValue(value.get("reviewNumber")));
        long interval = Math.max(1L, longValue(value.get("intervalSeconds")));
        String timing = String.valueOf(value.get("timingStatus"));
        try { java.util.UUID.fromString(sessionId); } catch (RuntimeException ex) { return; }
        if (wordId <= 0 || !wordExists(wordId) || !("EN_TO_ZH".equals(direction) || "ZH_TO_EN".equals(direction))) return;
        if (!List.of("NEW", "EARLY", "ON_TIME", "OVERDUE").contains(timing)) timing = "NEW";
        LocalDateTime reviewed = parseClientTime(value.get("reviewedAt"), now);
        if (reviewed == null || reviewed.isAfter(now.plusMinutes(5))) reviewed = now;
        LocalDateTime scheduled = parseClientTime(value.get("scheduledAt"), now);
        jdbc.update("""
                INSERT IGNORE INTO account_vocabulary_review_log
                (account_id,word_id,review_session_id,direction,review_number,scheduled_at,reviewed_at,
                 interval_seconds,timing_status) VALUES (?,?,?,?,?,?,?,?,?)
                """, accountId, wordId, sessionId, direction, reviewNumber, scheduled, reviewed, interval, timing);
    }

    public static long intervalSeconds(int reviewNumber) {
        int index = Math.max(1, Math.min(reviewNumber, REVIEW_INTERVAL_SECONDS.length)) - 1;
        return REVIEW_INTERVAL_SECONDS[index];
    }

    private void ensureSettings(long accountId) {
        jdbc.update("INSERT IGNORE INTO account_vocabulary_study_setting(account_id) VALUES (?)", accountId);
    }

    private VocabularyMemoryView memory(long accountId, long wordId) {
        try {
            return jdbc.queryForObject("""
                    SELECT m.word_id,m.memory_count,m.review_step,m.review_count,m.first_learned_at,
                           m.last_reviewed_at,m.next_review_at,m.learning_status,p.display_mode
                    FROM account_vocabulary_memory m
                    LEFT JOIN account_vocabulary_card_preference p
                      ON p.account_id=m.account_id AND p.word_id=m.word_id
                    WHERE m.account_id=? AND m.word_id=?
                    """, (rs, rowNum) -> memoryView(rs), accountId, wordId);
        } catch (EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VOCABULARY_PROGRESS_NOT_FOUND", "Progress not found",
                    "This word has not been added to the memory plan.");
        }
    }

    private VocabularyMemoryView optionalMemory(long accountId, long wordId, String displayMode) {
        try { return memory(accountId, wordId); }
        catch (ApiException ex) { return new VocabularyMemoryView(wordId, 0, 0, 0, null, null, null, "NEW", displayMode); }
    }

    private VocabularyMemoryView memoryView(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new VocabularyMemoryView(rs.getLong("word_id"), rs.getInt("memory_count"),
                rs.getInt("review_step"), rs.getInt("review_count"),
                format(rs.getObject("first_learned_at", LocalDateTime.class)),
                format(rs.getObject("last_reviewed_at", LocalDateTime.class)),
                format(rs.getObject("next_review_at", LocalDateTime.class)),
                rs.getString("learning_status") == null ? "NEW" : rs.getString("learning_status"),
                rs.getString("display_mode"));
    }

    private String timing(LocalDateTime scheduled, LocalDateTime reviewed) {
        if (scheduled == null) return "NEW";
        if (reviewed.isBefore(scheduled)) return "EARLY";
        return reviewed.isAfter(scheduled.plusHours(24)) ? "OVERDUE" : "ON_TIME";
    }

    private long count(String sql, long accountId) {
        return jdbc.queryForObject(sql, Long.class, accountId);
    }

    private int number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private long longNumber(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
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

    private boolean wordExists(long wordId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM vocabulary_word WHERE id=?", Integer.class, wordId) > 0;
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

    private LocalDateTime dateTime(Object value) {
        if (value instanceof LocalDateTime time) return time;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        return null;
    }

    private Object[] prepend(long accountId, List<Long> ids) {
        Object[] values = new Object[ids.size() + 2];
        values[0] = accountId;
        values[1] = accountId;
        for (int i = 0; i < ids.size(); i++) values[i + 2] = ids.get(i);
        return values;
    }

    private void requireSameReviewWord(long wordId, Map<String, Object> row) {
        if (longNumber(row, "word_id") != wordId) {
            throw new ApiException(HttpStatus.CONFLICT, "VOCABULARY_REVIEW_SESSION_REUSED",
                    "Review session already used", "The review session id belongs to another word.");
        }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private ZoneId siteZone() {
        return timezone.zone();
    }

    private String format(LocalDateTime value) {
        return value == null ? null : timezone.atSite(value).format(ISO_OFFSET);
    }
}
