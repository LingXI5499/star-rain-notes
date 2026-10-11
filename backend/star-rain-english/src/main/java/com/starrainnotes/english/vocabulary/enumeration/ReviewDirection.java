/*
 * ReviewDirection：英语学习模块的枚举文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 * 阅读入口：isKnown（判断是否Known）；isConcrete（判断是否Concrete）。
 */

package com.starrainnotes.english.vocabulary.enumeration;

/*
 * 复习方向。MIXED 只在设置里存在，落到具体卡片前必须解析成确定方向。
 */
/**
 * 审核Direction，属于英语学习模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum ReviewDirection {
    /* 英译中 */
    /**
     * ZH。
     */
    EN_TO_ZH,
    /* 中译英 */
    /**
     * EN。
     */
    ZH_TO_EN,
    /* 听音辨词 */
    /**
     * BOTH。
     */
    AUDIO_TO_BOTH,
    /* 随机混合（按 单词ID + 当天序数 稳定取向，同一天同一词方向不变） */
    /**
     * MIXED。
     */
    MIXED;

    /**
     * 判断是否Known。
     * 关键处理步骤调用 <code>values</code>、<code>direction.name</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
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

    /**
     * 判断是否Concrete。
     * 主要协作调用：<code>EN_TO_ZH.name</code>、<code>ZH_TO_EN.name</code>、<code>AUDIO_TO_BOTH.name</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    public static boolean isConcrete(String value) {
        return EN_TO_ZH.name().equals(value) || ZH_TO_EN.name().equals(value) || AUDIO_TO_BOTH.name().equals(value);
    }
}
