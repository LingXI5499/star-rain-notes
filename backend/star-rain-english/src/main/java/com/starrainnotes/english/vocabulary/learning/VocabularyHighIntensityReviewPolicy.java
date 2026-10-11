/*
 * VocabularyHighIntensityReviewPolicy：英语学习模块的模块支持文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 阅读入口：nextStep（下一项Step处理入口）；intervalSeconds（下一次复习的间隔，单位为秒处理入口）；timing（调度处理入口）。
 */

package com.starrainnotes.english.vocabulary.learning;

import java.time.Duration;
import java.time.LocalDateTime;

/** Configurable-in-code product intervals; no scheduler and no automatic overdue progression. */
public final class VocabularyHighIntensityReviewPolicy {
    /**
     * STEP。
     * 初始值为 <code>14</code>。
     */
    public static final int MASTERED_STEP = 14;
    /**
     * 秒。
     */
    private static final long[] INTERVAL_SECONDS = {
        300, 1200, 3600, 14400, 43200, 86400, 172800, 259200,
        432000, 604800, 864000, 1209600, 1814400, 2419200
    };
    /**
     * 秒。
     */
    public static final long MASTERED_INTERVAL_SECONDS = 35L * 86400;
    /**
     * 构造VocabularyHighIntensityReviewPolicy，接收本类使用的依赖或初始化参数。
     */
    private VocabularyHighIntensityReviewPolicy() { }

    /**
     * 下一项Step处理入口。
     * 关键处理步骤调用 <code>Math.max</code>、<code>Math.min</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     *
     * @param previous 上一项；类型为 <code>int</code>
     * @param rating 评分；类型为 <code>String</code>
     * @param graduated graduated；类型为 <code>boolean</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    public static int nextStep(int previous, String rating, boolean graduated) {
        int current = Math.max(-1, Math.min(MASTERED_STEP, previous));
        return switch (rating) {
            case "FORGOT" -> 0;
            case "UNCERTAIN" -> current == MASTERED_STEP ? 2 : Math.max(0, current - 2);
            case "KNOW" -> graduated ? MASTERED_STEP : Math.min(13, current + 1);
            default -> throw new IllegalArgumentException("Unknown rating");
        };
    }

    /**
     * 下一次复习的间隔，单位为秒处理入口。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     *
     * @param step step；类型为 <code>int</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    public static long intervalSeconds(int step) {
        if (step == MASTERED_STEP) { return MASTERED_INTERVAL_SECONDS; }
        if (step < 0 || step >= INTERVAL_SECONDS.length) { throw new IllegalArgumentException("Unknown step"); }
        return INTERVAL_SECONDS[step];
    }

    /**
     * 调度处理入口。
     * 关键处理步骤调用 <code>Duration.between</code>。
     *
     * @param due due；类型为 <code>LocalDateTime</code>
     * @param now now；类型为 <code>LocalDateTime</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String timing(LocalDateTime due, LocalDateTime now) {
        if (due == null) { return "NEW"; }
        long seconds = Duration.between(due, now).getSeconds();
        return seconds < 0 ? "EARLY" : seconds <= 300 ? "ON_TIME" : "OVERDUE";
    }
}
