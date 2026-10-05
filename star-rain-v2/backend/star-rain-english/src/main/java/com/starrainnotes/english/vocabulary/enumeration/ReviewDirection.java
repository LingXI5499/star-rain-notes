package com.starrainnotes.english.vocabulary.enumeration;

/*
 * 复习方向。MIXED 只在设置里存在，落到具体卡片前必须解析成确定方向。
 */
public enum ReviewDirection {
    /* 英译中 */
    EN_TO_ZH,
    /* 中译英 */
    ZH_TO_EN,
    /* 随机混合（按 单词ID + 当天序数 稳定取向，同一天同一词方向不变） */
    MIXED;

    public static boolean isKnown(String value) {
        if (value == null) {
            return false;
        }
        for (ReviewDirection direction : values()) {
            if (direction.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isConcrete(String value) {
        return EN_TO_ZH.name().equals(value) || ZH_TO_EN.name().equals(value);
    }
}
