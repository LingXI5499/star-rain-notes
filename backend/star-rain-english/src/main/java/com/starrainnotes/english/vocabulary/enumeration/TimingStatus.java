/*
 * TimingStatus：英语学习模块的枚举文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 * 阅读入口：isKnown（判断是否Known）。
 */

package com.starrainnotes.english.vocabulary.enumeration;

/*
 * 复习时机判定。口径与 V1 account_vocabulary_review_log.timing_status 一致：
 *   NEW      首次复习（没有计划时间）
 *   EARLY    早于计划时间
 *   ON_TIME  在计划时间之后 24 小时内
 *   OVERDUE  超过计划时间 24 小时
 */
/**
 * 调度状态，属于英语学习模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum TimingStatus {
    /**
     * NEW。
     */
    NEW,
    /**
     * EARLY。
     */
    EARLY,
    /**
     * 时间。
     */
    ON_TIME,
    /**
     * OVERDUE。
     */
    OVERDUE;

    /**
     * 判断是否Known。
     * 关键处理步骤调用 <code>values</code>、<code>status.name</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    public static boolean isKnown(String value) {
        if (value == null) {
            return false;
        }
        for (TimingStatus status : values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
