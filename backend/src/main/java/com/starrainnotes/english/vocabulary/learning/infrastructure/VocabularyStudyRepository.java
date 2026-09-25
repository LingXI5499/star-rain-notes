package com.starrainnotes.english.vocabulary.learning.infrastructure;

import com.starrainnotes.english.vocabulary.learning.domain.ReviewSchedule;
import com.starrainnotes.english.vocabulary.learning.VocabularyMemoryView;
import com.starrainnotes.english.vocabulary.learning.VocabularyStudySettingsRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyStudySettingsView;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class VocabularyStudyRepository {
    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;

    public VocabularyStudyRepository(JdbcTemplate jdbc, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.timezone = timezone;
    }

    public VocabularyStudySettingsView settings(long accountId) {
        java.util.List<VocabularyStudySettingsView> rows = jdbc.query("""
                SELECT show_english,show_chinese,review_direction,daily_new_limit,daily_review_limit
                FROM account_vocabulary_study_setting WHERE account_id=?
                """, (rs, ignored) -> new VocabularyStudySettingsView(rs.getBoolean("show_english"),
                rs.getBoolean("show_chinese"), rs.getString("review_direction"), rs.getInt("daily_new_limit"),
                rs.getInt("daily_review_limit")), accountId);
        if (rows.isEmpty()) {
            return new VocabularyStudySettingsView(true, true, "MIXED", 20, 200);
        }
        return rows.get(0);
    }

    public List<Map<String,Object>> memorySnapshot(long accountId) {
        return jdbc.queryForList("""
                SELECT word_id,memory_count,last_memory_at FROM account_vocabulary_memory
                WHERE account_id=? ORDER BY word_id
                """,accountId);
    }

    public void putMemoryCount(long accountId,long wordId,int memoryCount) {
        int updated=jdbc.update("""
                UPDATE account_vocabulary_memory SET memory_count=?,last_memory_at=UTC_TIMESTAMP(6)
                WHERE account_id=? AND word_id=?
                """,memoryCount,accountId,wordId);
        if(updated==0) jdbc.update("""
                INSERT INTO account_vocabulary_memory(account_id,word_id,memory_count,last_memory_at)
                VALUES (?,?,?,UTC_TIMESTAMP(6))
                """,accountId,wordId,memoryCount);
    }

    public void updateSettings(long accountId, VocabularyStudySettingsRequest request) {
        jdbc.update("""
                INSERT INTO account_vocabulary_study_setting
                    (account_id,show_english,show_chinese,review_direction,daily_new_limit,daily_review_limit)
                VALUES (?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE show_english=VALUES(show_english),show_chinese=VALUES(show_chinese),
                    review_direction=VALUES(review_direction),daily_new_limit=VALUES(daily_new_limit),
                    daily_review_limit=VALUES(daily_review_limit)
                """,accountId,request.showEnglish(),request.showChinese(),request.reviewDirection(),
                request.dailyNewLimit(),request.dailyReviewLimit());
    }

    public List<VocabularyMemoryView> memories(long accountId, List<Long> ids) {
        if(ids.isEmpty()) return List.of();
        String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
        Object[] args=new Object[ids.size()+2];
        args[0]=accountId; args[1]=accountId;
        for(int i=0;i<ids.size();i++) args[i+2]=ids.get(i);
        return jdbc.query("""
                SELECT w.id AS word_id,COALESCE(m.memory_count,0) AS memory_count,
                       COALESCE(m.review_step,0) AS review_step,COALESCE(m.review_count,0) AS review_count,
                       m.first_learned_at,m.last_reviewed_at,m.next_review_at,m.learning_status,p.display_mode
                FROM vocabulary_word w
                LEFT JOIN account_vocabulary_memory m ON m.word_id=w.id AND m.account_id=?
                LEFT JOIN account_vocabulary_card_preference p ON p.word_id=w.id AND p.account_id=?
                WHERE w.id IN (%s) ORDER BY w.id
                """.formatted(placeholders),(rs,ignored)->memoryView(rs),args);
    }

    public VocabularyMemoryView memory(long accountId,long wordId) {
        try {
            return jdbc.queryForObject("""
                    SELECT m.word_id,m.memory_count,m.review_step,m.review_count,m.first_learned_at,
                           m.last_reviewed_at,m.next_review_at,m.learning_status,p.display_mode
                    FROM account_vocabulary_memory m
                    LEFT JOIN account_vocabulary_card_preference p
                      ON p.account_id=m.account_id AND p.word_id=m.word_id
                    WHERE m.account_id=? AND m.word_id=?
                    """,(rs,ignored)->memoryView(rs),accountId,wordId);
        } catch(EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND,"VOCABULARY_PROGRESS_NOT_FOUND","Progress not found",
                    "This word has not been added to the memory plan.");
        }
    }

    private VocabularyMemoryView memoryView(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new VocabularyMemoryView(rs.getLong("word_id"),rs.getInt("memory_count"),
                rs.getInt("review_step"),rs.getInt("review_count"),
                format(rs.getObject("first_learned_at",LocalDateTime.class)),
                format(rs.getObject("last_reviewed_at",LocalDateTime.class)),
                format(rs.getObject("next_review_at",LocalDateTime.class)),
                rs.getString("learning_status")==null?"NEW":rs.getString("learning_status"),
                rs.getString("display_mode"));
    }

    private String format(LocalDateTime value) {
        return value==null?null:timezone.atSite(value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public int dueCount(long accountId, LocalDateTime now) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM account_vocabulary_memory
                WHERE account_id=? AND learning_status='ACTIVE' AND next_review_at<=?
                """,Integer.class,accountId,now);
    }

    public List<Long> dueIds(long accountId, LocalDateTime now, int limit) {
        return jdbc.queryForList("""
                SELECT word_id FROM account_vocabulary_memory
                WHERE account_id=? AND learning_status='ACTIVE' AND next_review_at<=?
                ORDER BY next_review_at,word_id LIMIT ?
                """,Long.class,accountId,now,limit);
    }

    public int introducedSince(long accountId,LocalDateTime start) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM account_vocabulary_memory
                WHERE account_id=? AND first_learned_at>=?
                """,Integer.class,accountId,start);
    }

    public List<Long> newWordIds(long themeId,long accountId,int limit) {
        return jdbc.queryForList("""
                SELECT w.id FROM vocabulary_word w
                WHERE w.theme_id=? AND NOT EXISTS (
                    SELECT 1 FROM account_vocabulary_memory m
                    WHERE m.account_id=? AND m.word_id=w.id
                )
                ORDER BY w.sort_order,w.id LIMIT ?
                """,Long.class,themeId,accountId,limit);
    }

    public long activeCount(long accountId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM account_vocabulary_memory WHERE account_id=? AND learning_status='ACTIVE'",Long.class,accountId);
    }

    public long dueCountLong(long accountId,LocalDateTime now) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM account_vocabulary_memory WHERE account_id=? AND learning_status='ACTIVE' AND next_review_at<=?",Long.class,accountId,now);
    }

    public long totalMemoryCount(long accountId) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(memory_count),0) FROM account_vocabulary_memory WHERE account_id=?",Long.class,accountId);
    }

    public void start(long accountId,long wordId,LocalDateTime now) {
        jdbc.update("""
                INSERT INTO account_vocabulary_memory
                    (account_id,word_id,memory_count,review_step,review_count,first_learned_at,
                     last_reviewed_at,next_review_at,learning_status,last_memory_at)
                VALUES (?,?,0,0,0,?,NULL,?,'ACTIVE',NULL)
                ON DUPLICATE KEY UPDATE learning_status='ACTIVE',
                    first_learned_at=COALESCE(first_learned_at,VALUES(first_learned_at)),
                    next_review_at=COALESCE(next_review_at,VALUES(next_review_at))
                """,accountId,wordId,now,now);
    }

    public void reset(long accountId,long wordId) {
        jdbc.update("DELETE FROM account_vocabulary_memory WHERE account_id=? AND word_id=?",accountId,wordId);
    }

    public void setDisplay(long accountId,long wordId,String displayMode) {
        jdbc.update("""
                INSERT INTO account_vocabulary_card_preference(account_id,word_id,display_mode)
                VALUES (?,?,?) ON DUPLICATE KEY UPDATE display_mode=VALUES(display_mode)
                """,accountId,wordId,displayMode);
    }

    public void clearDisplay(long accountId,long wordId) {
        jdbc.update("DELETE FROM account_vocabulary_card_preference WHERE account_id=? AND word_id=?",accountId,wordId);
    }

    public boolean wordExists(long wordId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM vocabulary_word WHERE id=?",Integer.class,wordId)>0;
    }

    public void importMemory(long accountId,long wordId,int memoryCount,int reviewCount,
                             LocalDateTime first,LocalDateTime last,LocalDateTime next,LocalDateTime now) {
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
                """,accountId,wordId,memoryCount,Math.min(reviewCount,10),reviewCount,
                first==null?now:first,last,next==null?now:next,last);
    }

    public Optional<MemoryLock> lockMemory(long accountId, long wordId) {
        List<MemoryLock> rows = jdbc.query("""
                SELECT memory_count,review_count,next_review_at FROM account_vocabulary_memory
                WHERE account_id=? AND word_id=? FOR UPDATE
                """,(rs,ignored)->new MemoryLock(rs.getInt("memory_count"),rs.getInt("review_count"),
                rs.getObject("next_review_at",LocalDateTime.class)),accountId,wordId);
        return rows.stream().findFirst();
    }

    public record MemoryLock(int memoryCount,int reviewCount,LocalDateTime nextReviewAt) {}

    public void initializeForReview(long accountId, long wordId, LocalDateTime now) {
        jdbc.update("""
                INSERT INTO account_vocabulary_memory
                (account_id,word_id,memory_count,review_step,review_count,first_learned_at,next_review_at,learning_status)
                VALUES (?,?,0,0,0,?,NULL,'ACTIVE')
                """, accountId, wordId, now);
    }

    public void advance(long accountId, long wordId, ReviewSchedule schedule) {
        LocalDateTime now = schedule.reviewedAt();
        jdbc.update("""
                UPDATE account_vocabulary_memory SET memory_count=memory_count+1,
                    review_step=?,review_count=?,first_learned_at=COALESCE(first_learned_at,?),
                    last_reviewed_at=?,last_memory_at=?,next_review_at=?,learning_status='ACTIVE'
                WHERE account_id=? AND word_id=?
                """, schedule.reviewStep(), schedule.reviewNumber(), now, now, now,
                schedule.nextReviewAt(), accountId, wordId);
    }
}
