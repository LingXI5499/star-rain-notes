package com.starrainnotes.english.vocabulary.enumeration;

/*
 * 记忆计划状态。语义与 V1 account_vocabulary_memory.learning_status 一致。
 */
public enum LearningStatus {
    /* 已建行但未进入记忆计划 */
    NEW,
    /* 计划内，会参与到期复习 */
    ACTIVE,
    /* 已暂停，不计入到期 */
    PAUSED;

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
