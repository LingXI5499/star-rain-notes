/*
 * VocabularyStudyDirectionPolicy：英语学习模块的工具文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：ReviewDirection。
 * 阅读入口：direction（direction处理入口）。
 */

package com.starrainnotes.english.vocabulary.utils;

import com.starrainnotes.english.vocabulary.enumeration.ReviewDirection;

/*
 * 复习方向解析。MIXED 必须落到确定方向，且同一天同一词方向稳定：
 * 用 (wordId + 当天序数) 的奇偶决定，与 V1 StudyDirectionPolicy 完全一致。
 * 这样做的好处是「同一批队列刷新两次得到同一方向」，用户不会看到卡片正反面随机翻转。
 */
/**
 * 词汇学习Direction策略，属于英语学习模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class VocabularyStudyDirectionPolicy {

    /**
     * 构造VocabularyStudyDirectionPolicy，接收本类使用的依赖或初始化参数。
     */
    private VocabularyStudyDirectionPolicy() { }

    /**
     * direction处理入口。
     * 关键处理步骤调用 <code>ReviewDirection.MIXED.name</code>、<code>ReviewDirection.EN_TO_ZH.name</code>、<code>ReviewDirection.ZH_TO_EN.name</code>。
     * 边界判断包含 <code>(!ReviewDirection.MIXED.name().equals(setting))</code>。
     *
     * @param setting setting；类型为 <code>String</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     * @param epochDay epoch日；类型为 <code>long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String direction(String setting, long wordId, long epochDay) {
        if (!ReviewDirection.MIXED.name().equals(setting)) {
            return setting;
        }
        return ((wordId + epochDay) & 1L) == 0
                ? ReviewDirection.EN_TO_ZH.name()
                : ReviewDirection.ZH_TO_EN.name();
    }
}
