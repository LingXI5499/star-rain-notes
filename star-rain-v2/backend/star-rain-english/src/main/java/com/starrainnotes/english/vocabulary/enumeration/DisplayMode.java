package com.starrainnotes.english.vocabulary.enumeration;

/*
 * 词卡显示模式。
 *
 * 全局设置用 show_english / show_chinese 两个布尔表达，单卡覆盖用这里的枚举；
 * 前端把二者归一成 BILINGUAL / ENGLISH_ONLY / CHINESE_ONLY 三档。
 */
public enum DisplayMode {
    BILINGUAL,
    ENGLISH_ONLY,
    CHINESE_ONLY
}
