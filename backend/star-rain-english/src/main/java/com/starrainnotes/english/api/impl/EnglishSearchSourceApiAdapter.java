/*
 * EnglishSearchSourceApiAdapter：英语学习模块的跨模块接口文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：EnglishSearchSourceApi、EnglishSearchTypes、EnglishSearchDocument、EnglishSearchSourceMapper。
 * 阅读入口：page（分页读取英语搜索来源接口Adapter）；findPublished（查找已发布）。
 */

package com.starrainnotes.english.api.impl;

import com.starrainnotes.english.api.EnglishSearchSourceApi;
import com.starrainnotes.english.api.EnglishSearchTypes;
import com.starrainnotes.english.api.dto.EnglishSearchDocument;
import com.starrainnotes.english.knowledge.mapper.EnglishSearchSourceMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 英语搜索来源接口Adapter，属于英语学习模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnglishSearchSourceApiAdapter implements EnglishSearchSourceApi {
    /**
     * 数据映射。
     * 协作类型：<code>EnglishSearchSourceMapper</code>。
     */
    private final EnglishSearchSourceMapper mapper;

    /**
     * 分页读取英语搜索来源接口Adapter。
     * 主要协作调用：<code>mapper.page</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     *
     * @param contentType 内容类型或请求声明的 Content-Type；类型为 <code>String</code>
     * @param afterId 之后标识；类型为 <code>Long</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<EnglishSearchDocument> page(String contentType, Long afterId, int limit) {
        validateType(contentType);
        if (limit < 1 || limit > 100 || (afterId != null && afterId < 0)) {
            throw new IllegalArgumentException("Invalid English search page");
        }
        List<EnglishSearchDocument> documents = mapper.page(contentType, afterId, limit);
        documents.forEach(this::route);
        return documents;
    }

    /**
     * 查找已发布。
     * 主要协作调用：<code>mapper.findPublished</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     *
     * @param contentType 内容类型或请求声明的 Content-Type；类型为 <code>String</code>
     * @param contentId 内容标识；类型为 <code>Long</code>
     * @return 类型为 <code>EnglishSearchDocument</code> 的处理结果
     */
    @Override
    public EnglishSearchDocument findPublished(String contentType, Long contentId) {
        validateType(contentType);
        if (contentId == null || contentId <= 0) throw new IllegalArgumentException("Invalid English content id");
        EnglishSearchDocument document = mapper.findPublished(contentType, contentId);
        if (document != null) route(document);
        return document;
    }

    /**
     * 路由处理入口。
     * 关键处理步骤调用 <code>document.getContentType</code>、<code>document.getThemeId</code>、<code>document.getTitle</code>、<code>URLEncoder.encode</code>。
     *
     * @param document 文档；类型为 <code>EnglishSearchDocument</code>
     */
    private void route(EnglishSearchDocument document) {
        if ("ENGLISH_VOCABULARY_WORD".equals(document.getContentType())) {
            String prefix = "/english/vocabulary?status=all&q=";
            String query = document.getTitle();
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            // A long non-ASCII phrase may exceed the index URL limit after encoding.
            // A prefix query still includes the matching word on the theme page.
            while (prefix.length() + encoded.length() > 500) {
                query = query.substring(0, query.offsetByCodePoints(query.length(), -1));
                encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            }
            document.setRoutePath(prefix + encoded);
        }
    }

    /**
     * 校验类型。
     * 关键处理步骤调用 <code>EnglishSearchTypes.ALL.contains</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     *
     * @param type 类型；类型为 <code>String</code>
     */
    private void validateType(String type) {
        if (!EnglishSearchTypes.ALL.contains(type)) throw new IllegalArgumentException("Invalid English search type");
    }
}
