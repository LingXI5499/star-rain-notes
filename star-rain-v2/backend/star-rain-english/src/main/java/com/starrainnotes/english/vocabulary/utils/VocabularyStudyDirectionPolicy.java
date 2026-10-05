package com.starrainnotes.english.vocabulary.utils;

import com.starrainnotes.english.vocabulary.enumeration.ReviewDirection;

/*
 * 复习方向解析。MIXED 必须落到确定方向，且同一天同一词方向稳定：
 * 用 (wordId + 当天序数) 的奇偶决定，与 V1 StudyDirectionPolicy 完全一致。
 * 这样做的好处是「同一批队列刷新两次得到同一方向」，用户不会看到卡片正反面随机翻转。
 */
public final class VocabularyStudyDirectionPolicy {

    private VocabularyStudyDirectionPolicy() { }

    public static String direction(String setting, long wordId, long epochDay) {
        if (!ReviewDirection.MIXED.name().equals(setting)) {
            return setting;
        }
        return ((wordId + epochDay) & 1L) == 0
                ? ReviewDirection.EN_TO_ZH.name()
                : ReviewDirection.ZH_TO_EN.name();
    }
}
