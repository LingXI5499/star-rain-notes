/*
 * LearningStatus：英语学习模块的枚举文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 * 阅读入口：isKnown（判断是否Known）。
 */

package com.starrainnotes.english.vocabulary.enumeration;

/*
 * 记忆计划状态。语义与 V1 account_vocabulary_memory.learning_status 一致。
 */
/**
 * 学习状态，属于英语学习模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum LearningStatus {
    /* 已建行但未进入记忆计划 */
    /**
     * NEW。
     */
    NEW,
    /* 计划内，会参与到期复习 */
    /**
     * 有效。
     */
    ACTIVE,
    /* 已暂停，不计入到期 */
    /**
     * PAUSED。
     */
    PAUSED;

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
        for (LearningStatus status : values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
