/*
 * VocabularyReviewPolicy：英语学习模块的工具文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：VocabularyStudyConstants、VocabularyReviewSchedule、TimingStatus。
 * 阅读入口：next（下一项处理入口）；intervalSeconds（下一次复习的间隔，单位为秒处理入口）；timingStatus（调度状态处理入口）。
 */

package com.starrainnotes.english.vocabulary.utils;

import com.starrainnotes.english.vocabulary.constant.VocabularyStudyConstants;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewSchedule;
import com.starrainnotes.english.vocabulary.enumeration.TimingStatus;
import java.time.LocalDateTime;

/*
 * 固定间隔复习策略。逐值对齐 V1 VocabularyReviewPolicy：
 * 复习序号递增 -> 取固定间隔表的对应档位 -> 下次复习时间 = 本次复习时间 + 间隔。
 * 时机判定同样照抄 V1 口径，历史 interval_seconds / timing_status 才能连续。
 */
/**
 * 词汇审核策略，属于英语学习模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class VocabularyReviewPolicy {

    /**
     * 构造VocabularyReviewPolicy，接收本类使用的依赖或初始化参数。
     */
    private VocabularyReviewPolicy() { }

    /**
     * 下一项处理入口。
     * 关键处理步骤调用 <code>intervalSeconds</code>、<code>timingStatus</code>、<code>VocabularyReviewSchedule.builder</code>、<code>Math.min</code>。
     *
     * @param currentReviewCount 当前审核数量；类型为 <code>int</code>
     * @param scheduledAt 计划调度时间；类型为 <code>LocalDateTime</code>
     * @param reviewedAt 复习时间；类型为 <code>LocalDateTime</code>
     * @return 类型为 <code>VocabularyReviewSchedule</code> 的处理结果
     */
    public static VocabularyReviewSchedule next(int currentReviewCount, LocalDateTime scheduledAt,
                                               LocalDateTime reviewedAt) {
        int reviewNumber = currentReviewCount + 1;
        long interval = intervalSeconds(reviewNumber);
        String timing = timingStatus(scheduledAt, reviewedAt);
        return VocabularyReviewSchedule.builder()
                .reviewNumber(reviewNumber)
                .reviewStep(Math.min(reviewNumber, VocabularyStudyConstants.MAX_REVIEW_STEP))
                .intervalSeconds(interval)
                .scheduledAt(scheduledAt)
                .reviewedAt(reviewedAt)
                .nextReviewAt(reviewedAt.plusSeconds(interval))
                .timingStatus(timing)
                .build();
    }

    /*
     * 第 reviewNumber 次复习使用的间隔秒数；序号越界时钳到 1..10。
     */
    /**
     * 下一次复习的间隔，单位为秒处理入口。
     * 关键处理步骤调用 <code>Math.max</code>、<code>Math.min</code>。
     *
     * @param reviewNumber 审核请求的业务编号；类型为 <code>int</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    public static long intervalSeconds(int reviewNumber) {
        int index = Math.max(1, Math.min(reviewNumber, VocabularyStudyConstants.MAX_REVIEW_STEP)) - 1;
        return VocabularyStudyConstants.REVIEW_INTERVAL_SECONDS[index];
    }

    /*
     * 时机判定：无计划时间为 NEW；早于计划为 EARLY；超过计划 24 小时为 OVERDUE；其余 ON_TIME。
     */
    /**
     * 调度状态处理入口。
     * 关键处理步骤调用 <code>TimingStatus.NEW.name</code>、<code>reviewedAt.isBefore</code>、<code>TimingStatus.EARLY.name</code>、<code>reviewedAt.isAfter</code>。
     * 边界判断包含 <code>(scheduledAt == null)</code>；<code>(reviewedAt.isBefore(scheduledAt))</code>。
     *
     * @param scheduledAt 计划调度时间；类型为 <code>LocalDateTime</code>
     * @param reviewedAt 复习时间；类型为 <code>LocalDateTime</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String timingStatus(LocalDateTime scheduledAt, LocalDateTime reviewedAt) {
        if (scheduledAt == null) {
            return TimingStatus.NEW.name();
        }
        if (reviewedAt.isBefore(scheduledAt)) {
            return TimingStatus.EARLY.name();
        }
        if (reviewedAt.isAfter(scheduledAt.plusSeconds(VocabularyStudyConstants.OVERDUE_GRACE_SECONDS))) {
            return TimingStatus.OVERDUE.name();
        }
        return TimingStatus.ON_TIME.name();
    }
}
