package com.starrainnotes.english.vocabulary.enumeration;

/*
 * 复习时机判定。口径与 V1 account_vocabulary_review_log.timing_status 一致：
 *   NEW      首次复习（没有计划时间）
 *   EARLY    早于计划时间
 *   ON_TIME  在计划时间之后 24 小时内
 *   OVERDUE  超过计划时间 24 小时
 */
public enum TimingStatus {
    NEW,
    EARLY,
    ON_TIME,
    OVERDUE;

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
