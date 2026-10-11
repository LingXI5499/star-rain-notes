/*
 * EnglishSearchTypes：英语学习模块的跨模块接口文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */

package com.starrainnotes.english.api;

import java.util.List;

/**
 * 英语搜索Types，属于英语学习模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public final class EnglishSearchTypes {
    /**
     * 全部。
     */
    public static final List<String> ALL = List.of("ENGLISH_VOCABULARY_THEME", "ENGLISH_VOCABULARY_WORD",
        "ENGLISH_GRAMMAR_COURSE", "ENGLISH_GRAMMAR_LESSON", "ENGLISH_READING", "ENGLISH_WRITING");

    /**
     * 构造EnglishSearchTypes，接收本类使用的依赖或初始化参数。
     */
    private EnglishSearchTypes() { }
}
