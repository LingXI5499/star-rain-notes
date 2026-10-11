/*
 * DisplayMode：英语学习模块的枚举文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */

package com.starrainnotes.english.vocabulary.enumeration;

/*
 * 词卡显示模式。
 *
 * 全局设置用 show_english / show_chinese 两个布尔表达，单卡覆盖用这里的枚举；
 * 前端把二者归一成 BILINGUAL / ENGLISH_ONLY / CHINESE_ONLY 三档。
 */
/**
 * 显示Mode，属于英语学习模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum DisplayMode {
    /**
     * BILINGUAL。
     */
    BILINGUAL,
    /**
     * ONLY。
     */
    ENGLISH_ONLY,
    /**
     * ONLY。
     */
    CHINESE_ONLY
}
