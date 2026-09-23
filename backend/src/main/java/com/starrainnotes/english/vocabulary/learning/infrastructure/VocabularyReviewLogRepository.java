package com.starrainnotes.english.vocabulary.learning.infrastructure;

import com.starrainnotes.english.vocabulary.learning.domain.ReviewSchedule;
import com.starrainnotes.english.vocabulary.learning.VocabularyReviewHistoryView;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Repository
public class VocabularyReviewLogRepository {
    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;

    public VocabularyReviewLogRepository(JdbcTemplate jdbc, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.timezone = timezone;
    }

    public Optional<ReviewLogResult> findBySession(long accountId, String sessionId) {
        List<ReviewLogResult> rows = jdbc.query("""
                SELECT word_id,review_number,interval_seconds,timing_status FROM account_vocabulary_review_log
                WHERE account_id=? AND review_session_id=? LIMIT 1
                """,(rs,ignored)->new ReviewLogResult(rs.getLong("word_id"),rs.getInt("review_number"),
                rs.getLong("interval_seconds"),rs.getString("timing_status")),accountId,sessionId);
        return rows.stream().findFirst();
    }

    public record ReviewLogResult(long wordId,int reviewNumber,long intervalSeconds,String timingStatus) {}

    public boolean insert(long accountId, long wordId, String sessionId, String direction, ReviewSchedule schedule) {
        return jdbc.update("""
                INSERT IGNORE INTO account_vocabulary_review_log
                (account_id,word_id,review_session_id,direction,review_number,scheduled_at,reviewed_at,
                 interval_seconds,timing_status)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, accountId, wordId, sessionId, direction, schedule.reviewNumber(),
                schedule.scheduledAt(), schedule.reviewedAt(), schedule.intervalSeconds(), schedule.timingStatus()) > 0;
    }

    public long count(long accountId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM account_vocabulary_review_log WHERE account_id=?",Long.class,accountId);
    }

    public void importReview(long accountId,long wordId,String sessionId,String direction,int reviewNumber,
                             LocalDateTime scheduled,LocalDateTime reviewed,long interval,String timing) {
        jdbc.update("""
                INSERT IGNORE INTO account_vocabulary_review_log
                (account_id,word_id,review_session_id,direction,review_number,scheduled_at,reviewed_at,
                 interval_seconds,timing_status) VALUES (?,?,?,?,?,?,?,?,?)
                """,accountId,wordId,sessionId,direction,reviewNumber,scheduled,reviewed,interval,timing);
    }

    public long countSince(long accountId,LocalDateTime start) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM account_vocabulary_review_log WHERE account_id=? AND reviewed_at>=?",Long.class,accountId,start);
    }

    public List<VocabularyReviewHistoryView> recent(long accountId) {
        return jdbc.query("""
                SELECT l.id,l.word_id,w.word,l.review_number,l.direction,l.scheduled_at,l.reviewed_at,
                       l.interval_seconds,l.timing_status
                FROM account_vocabulary_review_log l JOIN vocabulary_word w ON w.id=l.word_id
                WHERE l.account_id=? ORDER BY l.reviewed_at DESC,l.id DESC LIMIT 100
                """,(rs,ignored)->new VocabularyReviewHistoryView(rs.getLong("id"),rs.getLong("word_id"),
                rs.getString("word"),rs.getInt("review_number"),rs.getString("direction"),
                format(rs.getObject("scheduled_at",LocalDateTime.class)),
                format(rs.getObject("reviewed_at",LocalDateTime.class)),rs.getLong("interval_seconds"),
                rs.getString("timing_status")),accountId);
    }

    private String format(LocalDateTime value) {
        return value==null?null:timezone.atSite(value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
