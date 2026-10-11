/*
 * EnglishSearchSourceApi：英语学习模块的跨模块接口文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：EnglishSearchDocument。
 * 阅读入口：page（分页读取英语搜索来源）；findPublished（查找已发布）。
 */

package com.starrainnotes.english.api;

import com.starrainnotes.english.api.dto.EnglishSearchDocument;
import java.util.List;

/* Public content only; private writing and uncleared reading never cross this boundary. */
/**
 * 英语搜索来源接口，属于英语学习模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface EnglishSearchSourceApi {
    /**
     * 分页读取英语搜索来源。
     *
     * @param contentType 内容类型或请求声明的 Content-Type；类型为 <code>String</code>
     * @param afterId 之后标识；类型为 <code>Long</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    List<EnglishSearchDocument> page(String contentType, Long afterId, int limit);

    /**
     * 查找已发布。
     *
     * @param contentType 内容类型或请求声明的 Content-Type；类型为 <code>String</code>
     * @param contentId 内容标识；类型为 <code>Long</code>
     * @return 类型为 <code>EnglishSearchDocument</code> 的处理结果
     */
    EnglishSearchDocument findPublished(String contentType, Long contentId);
}
