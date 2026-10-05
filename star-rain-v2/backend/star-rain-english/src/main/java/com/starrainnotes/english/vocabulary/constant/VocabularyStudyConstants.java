package com.starrainnotes.english.vocabulary.constant;

/*
 * 词汇记忆体系的固定口径。
 *
 * 间隔表与时机判定是 V1 既有的产品行为，必须逐值对齐，不能另发明一套：
 * V1 VocabularyReviewPolicy.REVIEW_INTERVAL_SECONDS 与
 * account_vocabulary_review_log.interval_seconds 的历史数据都以此为口径。
 */
public final class VocabularyStudyConstants {

    private VocabularyStudyConstants() { }

    /*
     * 十阶段固定复习间隔（秒）：5 分、30 分、12 时、1 天、2 天、4 天、7 天、15 天、30 天、60 天。
     * 下标 = 复习序号 - 1，序号超过 10 后停在最后一档。
     */
    public static final long[] REVIEW_INTERVAL_SECONDS = {
            300L, 1_800L, 43_200L, 86_400L, 172_800L,
            345_600L, 604_800L, 1_296_000L, 2_592_000L, 5_184_000L
    };

    /* review_step 的档位上限，等于间隔表长度 */
    public static final int MAX_REVIEW_STEP = REVIEW_INTERVAL_SECONDS.length;

    /* OVERDUE 的门槛：超过计划时间 24 小时 */
    public static final long OVERDUE_GRACE_SECONDS = 86_400L;

    /* 学习设置缺省值（账户还没有设置行时返回，不写库） */
    public static final boolean DEFAULT_SHOW_ENGLISH = true;
    public static final boolean DEFAULT_SHOW_CHINESE = true;
    public static final String DEFAULT_REVIEW_DIRECTION = "MIXED";
    public static final int DEFAULT_DAILY_NEW_LIMIT = 20;
    public static final int DEFAULT_DAILY_REVIEW_LIMIT = 200;

    /* 单次批量状态查询的词数上限，避免 IN 列表无限增长 */
    public static final int MAX_STATE_QUERY_WORDS = 500;

    /* 单次批量取词的词数上限 */
    public static final int MAX_BATCH_WORDS = 100;

    /* 每日新词上限的允许区间 */
    public static final int MIN_DAILY_NEW_LIMIT = 0;
    public static final int MAX_DAILY_NEW_LIMIT = 200;

    /* 每日复习上限的允许区间 */
    public static final int MIN_DAILY_REVIEW_LIMIT = 1;
    public static final int MAX_DAILY_REVIEW_LIMIT = 1000;

    /* 进度页「最近真实复习」最多返回的条数 */
    public static final int RECENT_REVIEW_LIMIT = 100;

    /* 导入本机进度时允许的未来时间容差（客户端时钟漂移） */
    public static final long IMPORT_FUTURE_TOLERANCE_MINUTES = 5L;
}
