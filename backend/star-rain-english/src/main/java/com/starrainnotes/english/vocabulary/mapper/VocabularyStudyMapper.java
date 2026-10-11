package com.starrainnotes.english.vocabulary.mapper;

import com.starrainnotes.english.vocabulary.dto.VocabularyMemoryEntryRow;
import com.starrainnotes.english.vocabulary.dto.VocabularyMemoryLock;
import com.starrainnotes.english.vocabulary.dto.VocabularyMemoryRow;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewHistoryRow;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewLogRow;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewSchedule;
import com.starrainnotes.english.vocabulary.vo.VocabularyStudySettingsVO;
import com.starrainnotes.english.vocabulary.vo.VocabularySummaryVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 词汇记忆体系的持久化映射。所有个人数据语句都带 account_id 条件，跨账户读不到彼此的数据。
 */
@Mapper
public interface VocabularyStudyMapper {

    /* ---------- 学习设置 ---------- */

    VocabularyStudySettingsVO settings(@Param("accountId") long accountId);

    int upsertSettings(@Param("accountId") long accountId,
                       @Param("settings") VocabularyStudySettingsVO settings);

    /* ---------- 单卡显示偏好 ---------- */

    String displayMode(@Param("accountId") long accountId, @Param("wordId") long wordId);

    int upsertDisplay(@Param("accountId") long accountId, @Param("wordId") long wordId,
                      @Param("displayMode") String displayMode);

    int deleteDisplay(@Param("accountId") long accountId, @Param("wordId") long wordId);

    /* ---------- 记忆状态 ---------- */

    List<VocabularyMemoryRow> memories(@Param("accountId") long accountId,
                                       @Param("wordIds") List<Long> wordIds);

    VocabularyMemoryRow memory(@Param("accountId") long accountId, @Param("wordId") long wordId);

    List<VocabularyMemoryEntryRow> memorySnapshot(@Param("accountId") long accountId);

    VocabularySummaryVO summary(@Param("accountId") long accountId, @Param("now") LocalDateTime now,
                                @Param("maxStep") int maxStep);

    long dueCount(@Param("accountId") long accountId, @Param("now") LocalDateTime now);

    List<Long> dueWordIds(@Param("accountId") long accountId, @Param("now") LocalDateTime now,
                          @Param("limit") int limit);

    int introducedSince(@Param("accountId") long accountId, @Param("start") LocalDateTime start);

    List<Long> newWordIds(@Param("themeId") long themeId, @Param("accountId") long accountId,
                          @Param("limit") int limit);

    long totalMemoryCount(@Param("accountId") long accountId);

    long activeCount(@Param("accountId") long accountId);

    int startMemory(@Param("accountId") long accountId, @Param("wordId") long wordId,
                    @Param("now") LocalDateTime now);

    int initializeMemory(@Param("accountId") long accountId, @Param("wordId") long wordId,
                         @Param("now") LocalDateTime now);

    int resetMemory(@Param("accountId") long accountId, @Param("wordId") long wordId);

    VocabularyMemoryLock lockMemory(@Param("accountId") long accountId, @Param("wordId") long wordId);

    int advanceMemory(@Param("accountId") long accountId, @Param("wordId") long wordId,
                      @Param("schedule") VocabularyReviewSchedule schedule);

    int importMemory(@Param("accountId") long accountId, @Param("wordId") long wordId,
                     @Param("memoryCount") int memoryCount, @Param("reviewStep") int reviewStep,
                     @Param("reviewCount") int reviewCount, @Param("firstLearnedAt") LocalDateTime firstLearnedAt,
                     @Param("lastReviewedAt") LocalDateTime lastReviewedAt,
                     @Param("nextReviewAt") LocalDateTime nextReviewAt,
                     @Param("lastMemoryAt") LocalDateTime lastMemoryAt,
                     @Param("now") LocalDateTime now);

    int wordExists(@Param("wordId") long wordId);
    int calibrateLegacyMemory(@Param("accountId") long accountId, @Param("wordId") long wordId);

    /* ---------- 复习日志 ---------- */

    VocabularyReviewLogRow findReviewBySession(@Param("accountId") long accountId,
                                               @Param("reviewSessionId") String reviewSessionId);

    int insertReview(@Param("accountId") long accountId, @Param("wordId") long wordId,
                     @Param("reviewSessionId") String reviewSessionId, @Param("direction") String direction,
                     @Param("schedule") VocabularyReviewSchedule schedule);

    int importReview(@Param("accountId") long accountId, @Param("wordId") long wordId,
                     @Param("reviewSessionId") String reviewSessionId, @Param("direction") String direction,
                     @Param("reviewNumber") int reviewNumber, @Param("scheduledAt") LocalDateTime scheduledAt,
                     @Param("reviewedAt") LocalDateTime reviewedAt, @Param("intervalSeconds") long intervalSeconds,
                     @Param("timingStatus") String timingStatus);

    long countReviews(@Param("accountId") long accountId);

    long countReviewsSince(@Param("accountId") long accountId, @Param("start") LocalDateTime start);

    List<VocabularyReviewHistoryRow> recentReviews(@Param("accountId") long accountId,
                                                   @Param("limit") int limit);
}
