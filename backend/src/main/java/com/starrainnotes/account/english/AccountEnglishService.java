package com.starrainnotes.account.english;

import com.starrainnotes.common.error.ApiException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Phase 4 · per-account English learning data (records, vocabulary memory,
 * writing drafts), plus one-time claim of legacy anonymous progress. Public
 * guest writes are already fenced off (GUEST_PROGRESS_LOCAL_ONLY handled by the
 * controller/security boundary).
 */
@Service
public class AccountEnglishService {

    private final JdbcTemplate jdbc;

    public AccountEnglishService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public long ensureAccountProfile(Long accountId) {
        Long existing = jdbc.queryForObject(
                "SELECT id FROM english_learner_profile WHERE account_id=? LIMIT 1", Long.class, accountId);
        if (existing != null) return existing;
        jdbc.update("INSERT INTO english_learner_profile(learner_key_hash,account_id,profile_type,claimed_at)"
                + " VALUES (NULL,?, 'ACCOUNT', UTC_TIMESTAMP(6))", accountId);
        return jdbc.queryForObject(
                "SELECT id FROM english_learner_profile WHERE account_id=? LIMIT 1", Long.class, accountId);
    }

    public Long profileId(Long accountId) {
        try {
            return jdbc.queryForObject(
                    "SELECT id FROM english_learner_profile WHERE account_id=? LIMIT 1", Long.class, accountId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public List<Map<String, Object>> records(Long accountId, int page, int pageSize) {
        Long profile = profileId(accountId);
        if (profile == null) return List.of();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id,content_type,content_id,content_slug,cefr_level,completion_status,score,
                       time_spent_seconds,attempts,mastery_level,next_review_at,updated_at
                FROM english_learning_record WHERE learner_id=?
                ORDER BY updated_at DESC LIMIT ? OFFSET ?
                """, profile, Math.min(Math.max(pageSize, 1), 50), (Math.max(page, 1) - 1) * 50);
        return rows;
    }

    public Map<String, Object> record(Long accountId, String type, Long contentId) {
        Long profile = profileId(accountId);
        if (profile == null) throw fail("LEARNING_RECORD_NOT_FOUND");
        try {
            return jdbc.queryForMap("""
                    SELECT id,content_type,content_id,content_slug,cefr_level,completion_status,score,
                           time_spent_seconds,attempts,mastery_level,next_review_at,updated_at
                    FROM english_learning_record WHERE learner_id=? AND content_type=? AND content_id=?
                    """, profile, type, contentId);
        } catch (EmptyResultDataAccessException ex) {
            throw fail("LEARNING_RECORD_NOT_FOUND");
        }
    }

    @Transactional
    public void putRecord(Long accountId, String type, Long contentId,
                          String completionStatus, Integer timeSpentSeconds) {
        Long profile = ensureAccountProfile(accountId);
        int updated = jdbc.update("""
                UPDATE english_learning_record
                SET completion_status=?, time_spent_seconds=?, attempts=attempts+1, last_attempt_at=UTC_TIMESTAMP(6)
                WHERE learner_id=? AND content_type=? AND content_id=?
                """, completionStatus, timeSpentSeconds == null ? 0 : timeSpentSeconds, profile, type, contentId);
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO english_learning_record
                    (learner_id,content_type,content_id,content_slug,completion_status,time_spent_seconds,attempts,weak_points_json)
                    VALUES (?,?,?,?,?,?,1, JSON_ARRAY())
                    """, profile, type, contentId, type.toLowerCase() + "-" + contentId, completionStatus,
                    timeSpentSeconds == null ? 0 : timeSpentSeconds);
        }
    }

    public List<Map<String, Object>> vocabularyMemory(Long accountId) {
        return jdbc.queryForList("""
                SELECT word_id,memory_count,last_memory_at FROM account_vocabulary_memory
                WHERE account_id=? ORDER BY word_id
                """, accountId);
    }

    @Transactional
    public void putVocabularyMemory(Long accountId, Long wordId, int memoryCount) {
        if (memoryCount < 0) throw fail("INVALID_MEMORY_COUNT");
        int updated = jdbc.update("""
                UPDATE account_vocabulary_memory SET memory_count=?, last_memory_at=UTC_TIMESTAMP(6)
                WHERE account_id=? AND word_id=?
                """, memoryCount, accountId, wordId);
        if (updated == 0) {
            jdbc.update("INSERT INTO account_vocabulary_memory(account_id,word_id,memory_count,last_memory_at)"
                    + " VALUES (?,?,?,UTC_TIMESTAMP(6))", accountId, wordId, memoryCount);
        }
    }

    public Map<String, Object> writingSubmission(Long accountId, Long promptId) {
        try {
            return jdbc.queryForMap("""
                    SELECT * FROM english_writing_submission WHERE learner_id=?
                      AND prompt_id=? LIMIT 1
                    """, profileId(accountId), promptId);
        } catch (EmptyResultDataAccessException ex) {
            throw fail("WRITING_SUBMISSION_NOT_FOUND");
        }
    }

    @Transactional
    public long claimLegacyProgress(Long accountId, String learnerKeyHash) {
        Long profile = profileId(accountId);
        if (profile != null) return profile;
        Long legacy = jdbc.queryForObject(
                "SELECT id FROM english_learner_profile WHERE learner_key_hash=? LIMIT 1", Long.class, learnerKeyHash);
        if (legacy == null) throw fail("LEGACY_PROFILE_NOT_FOUND");
        jdbc.update("UPDATE english_learner_profile SET account_id=?, profile_type='ACCOUNT', claimed_at=UTC_TIMESTAMP(6)"
                + " WHERE id=?", accountId, legacy);
        return legacy;
    }

    private ApiException fail(String code) {
        return new ApiException(HttpStatus.NOT_FOUND, code, "Not found", "The requested data does not exist.");
    }
}