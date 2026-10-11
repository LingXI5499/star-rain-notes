/*
 * VocabularyStudyMapper：英语学习模块的数据访问文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：VocabularyMemoryEntryRow、VocabularyMemoryLock、VocabularyMemoryRow、VocabularyReviewHistoryRow、VocabularyReviewLogRow、VocabularyReviewSchedule、VocabularyStudySettingsVO。
 * 阅读入口：settings（settings处理入口）；upsertSettings（新增或更新Settings）；displayMode（显示Mode处理入口）；upsertDisplay（新增或更新显示）。
 */

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
/**
 * 词汇学习数据映射，属于英语学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface VocabularyStudyMapper {

    /* ---------- 学习设置 ---------- */

    /**
     * settings处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_study_setting</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 类型为 <code>VocabularyStudySettingsVO</code> 的处理结果
     */
    VocabularyStudySettingsVO settings(@Param("accountId") long accountId);

    /**
     * 新增或更新Settings。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_study_setting</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param settings settings；类型为 <code>VocabularyStudySettingsVO</code>；对应 MyBatis 命名参数 <code>@Param("settings")</code>
     * @return SQL 写入影响的记录行数
     */
    int upsertSettings(@Param("accountId") long accountId,
                       @Param("settings") VocabularyStudySettingsVO settings);

    /* ---------- 单卡显示偏好 ---------- */

    /**
     * 显示Mode处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_card_preference</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String displayMode(@Param("accountId") long accountId, @Param("wordId") long wordId);

    /**
     * 新增或更新显示。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_card_preference</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @param displayMode 显示Mode；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("displayMode")</code>
     * @return SQL 写入影响的记录行数
     */
    int upsertDisplay(@Param("accountId") long accountId, @Param("wordId") long wordId,
                      @Param("displayMode") String displayMode);

    /**
     * 删除显示。
     *
     * 持久化操作：删除，涉及 <code>sr_english_vocabulary_card_preference</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteDisplay(@Param("accountId") long accountId, @Param("wordId") long wordId);

    /* ---------- 记忆状态 ---------- */

    /**
     * memories处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_word</code>、<code>sr_english_vocabulary_memory</code>、<code>sr_english_vocabulary_card_preference</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordIds 单词标识集合；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("wordIds")</code>
     * @return 本次处理得到的条目集合
     */
    List<VocabularyMemoryRow> memories(@Param("accountId") long accountId,
                                       @Param("wordIds") List<Long> wordIds);

    /**
     * 记忆处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_memory</code>、<code>sr_english_vocabulary_card_preference</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @return 类型为 <code>VocabularyMemoryRow</code> 的处理结果
     */
    VocabularyMemoryRow memory(@Param("accountId") long accountId, @Param("wordId") long wordId);

    /**
     * 记忆快照处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<VocabularyMemoryEntryRow> memorySnapshot(@Param("accountId") long accountId);

    /**
     * 摘要处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_mode_memory</code>、<code>sr_english_vocabulary_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     * @param maxStep 最大Step；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("maxStep")</code>
     * @return 符合 SQL 条件的记录数量
     */
    VocabularySummaryVO summary(@Param("accountId") long accountId, @Param("now") LocalDateTime now,
                                @Param("maxStep") int maxStep);

    /**
     * due数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_mode_memory</code>、<code>sr_english_vocabulary_word</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long dueCount(@Param("accountId") long accountId, @Param("now") LocalDateTime now);

    /**
     * due单词标识集合处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_memory</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<Long> dueWordIds(@Param("accountId") long accountId, @Param("now") LocalDateTime now,
                          @Param("limit") int limit);

    /**
     * introducedSince处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param start 开始；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("start")</code>
     * @return 符合 SQL 条件的记录数量
     */
    int introducedSince(@Param("accountId") long accountId, @Param("start") LocalDateTime start);

    /**
     * new单词标识集合处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_word</code>、<code>sr_english_vocabulary_memory</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param themeId 主题标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("themeId")</code>
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<Long> newWordIds(@Param("themeId") long themeId, @Param("accountId") long accountId,
                          @Param("limit") int limit);

    /**
     * 总数记忆数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    long totalMemoryCount(@Param("accountId") long accountId);

    /**
     * 有效数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long activeCount(@Param("accountId") long accountId);

    /**
     * 开始记忆处理入口。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     * @return SQL 写入影响的记录行数
     */
    int startMemory(@Param("accountId") long accountId, @Param("wordId") long wordId,
                    @Param("now") LocalDateTime now);

    /**
     * initialize记忆处理入口。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     * @return SQL 写入影响的记录行数
     */
    int initializeMemory(@Param("accountId") long accountId, @Param("wordId") long wordId,
                         @Param("now") LocalDateTime now);

    /**
     * 重置记忆。
     *
     * 持久化操作：删除，涉及 <code>sr_english_vocabulary_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @return SQL 写入影响的记录行数
     */
    int resetMemory(@Param("accountId") long accountId, @Param("wordId") long wordId);

    /**
     * 锁记忆处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_memory</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @return 类型为 <code>VocabularyMemoryLock</code> 的处理结果
     */
    VocabularyMemoryLock lockMemory(@Param("accountId") long accountId, @Param("wordId") long wordId);

    /**
     * advance记忆处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_english_vocabulary_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @param schedule schedule；类型为 <code>VocabularyReviewSchedule</code>；对应 MyBatis 命名参数 <code>@Param("schedule")</code>
     * @return SQL 写入影响的记录行数
     */
    int advanceMemory(@Param("accountId") long accountId, @Param("wordId") long wordId,
                      @Param("schedule") VocabularyReviewSchedule schedule);

    /**
     * import记忆处理入口。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @param memoryCount 记忆数量；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("memoryCount")</code>
     * @param reviewStep 审核Step；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("reviewStep")</code>
     * @param reviewCount 审核数量；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("reviewCount")</code>
     * @param firstLearnedAt 首次学习时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("firstLearnedAt")</code>
     * @param lastReviewedAt 最近一次复习时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("lastReviewedAt")</code>
     * @param nextReviewAt 下一次计划复习时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("nextReviewAt")</code>
     * @param lastMemoryAt 最近记忆时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("lastMemoryAt")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     * @return SQL 写入影响的记录行数
     */
    int importMemory(@Param("accountId") long accountId, @Param("wordId") long wordId,
                     @Param("memoryCount") int memoryCount, @Param("reviewStep") int reviewStep,
                     @Param("reviewCount") int reviewCount, @Param("firstLearnedAt") LocalDateTime firstLearnedAt,
                     @Param("lastReviewedAt") LocalDateTime lastReviewedAt,
                     @Param("nextReviewAt") LocalDateTime nextReviewAt,
                     @Param("lastMemoryAt") LocalDateTime lastMemoryAt,
                     @Param("now") LocalDateTime now);

    /**
     * 单词存在性处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_word</code>。
     *
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    int wordExists(@Param("wordId") long wordId);
    /**
     * calibrateLegacy记忆处理入口。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_mode_memory</code>、<code>sr_english_vocabulary_memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @return SQL 写入影响的记录行数
     */
    int calibrateLegacyMemory(@Param("accountId") long accountId, @Param("wordId") long wordId);

    /* ---------- 复习日志 ---------- */

    /**
     * 查找审核依据会话。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_review_log</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param reviewSessionId 审核会话标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("reviewSessionId")</code>
     * @return 类型为 <code>VocabularyReviewLogRow</code> 的处理结果
     */
    VocabularyReviewLogRow findReviewBySession(@Param("accountId") long accountId,
                                               @Param("reviewSessionId") String reviewSessionId);

    /**
     * 写入审核。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_review_log</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @param reviewSessionId 审核会话标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("reviewSessionId")</code>
     * @param direction direction；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("direction")</code>
     * @param schedule schedule；类型为 <code>VocabularyReviewSchedule</code>；对应 MyBatis 命名参数 <code>@Param("schedule")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertReview(@Param("accountId") long accountId, @Param("wordId") long wordId,
                     @Param("reviewSessionId") String reviewSessionId, @Param("direction") String direction,
                     @Param("schedule") VocabularyReviewSchedule schedule);

    /**
     * import审核处理入口。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_review_log</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param wordId 单词标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("wordId")</code>
     * @param reviewSessionId 审核会话标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("reviewSessionId")</code>
     * @param direction direction；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("direction")</code>
     * @param reviewNumber 审核请求的业务编号；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("reviewNumber")</code>
     * @param scheduledAt 计划调度时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("scheduledAt")</code>
     * @param reviewedAt 复习时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("reviewedAt")</code>
     * @param intervalSeconds 下一次复习的间隔，单位为秒；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("intervalSeconds")</code>
     * @param timingStatus 调度状态；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("timingStatus")</code>
     * @return SQL 写入影响的记录行数
     */
    int importReview(@Param("accountId") long accountId, @Param("wordId") long wordId,
                     @Param("reviewSessionId") String reviewSessionId, @Param("direction") String direction,
                     @Param("reviewNumber") int reviewNumber, @Param("scheduledAt") LocalDateTime scheduledAt,
                     @Param("reviewedAt") LocalDateTime reviewedAt, @Param("intervalSeconds") long intervalSeconds,
                     @Param("timingStatus") String timingStatus);

    /**
     * 统计Reviews。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_review_log</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countReviews(@Param("accountId") long accountId);

    /**
     * 统计ReviewsSince。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_review_log</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param start 开始；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("start")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countReviewsSince(@Param("accountId") long accountId, @Param("start") LocalDateTime start);

    /**
     * recentReviews处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_review_log</code>、<code>sr_english_vocabulary_word</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<VocabularyReviewHistoryRow> recentReviews(@Param("accountId") long accountId,
                                                   @Param("limit") int limit);
}
