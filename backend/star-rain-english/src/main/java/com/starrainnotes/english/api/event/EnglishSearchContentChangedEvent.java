/*
 * EnglishSearchContentChangedEvent：英语学习模块的跨模块接口文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */

package com.starrainnotes.english.api.event;

import lombok.Value;

/* A null contentId requests reconciliation of a type after a parent content change. */
/**
 * 英语搜索内容Changed事件，属于英语学习模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Value
public class EnglishSearchContentChangedEvent {
    /**
     * 内容类型或请求声明的 Content-Type。
     */
    String contentType;
    /**
     * 内容标识。
     */
    Long contentId;
}
