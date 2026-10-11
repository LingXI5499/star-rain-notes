/*
 * SearchIndexService：搜索模块的业务契约文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：SearchIndexApi、SearchableDocument、SearchDocumentMapper、EnglishSearchTypes。
 * 阅读入口：upsert（新增或更新搜索索引）；remove（移除搜索索引）；removeByContent（移除依据内容）；removeChapters（移除章节列表）。
 */

package com.starrainnotes.search.service;

import com.starrainnotes.search.api.SearchIndexApi;
import com.starrainnotes.search.api.dto.SearchableDocument;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.english.api.EnglishSearchTypes;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 搜索索引业务服务，属于搜索模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
@Service
@RequiredArgsConstructor
public class SearchIndexService implements SearchIndexApi {
    /**
     * TYPES。
     */
    public static final Set<String> TYPES = Stream.concat(
        Stream.of("TUTORIAL", "CHAPTER", "BLOG", "PORTFOLIO", "PROFILE"), EnglishSearchTypes.ALL.stream())
        .collect(Collectors.toUnmodifiableSet());
    /**
     * 数据映射。
     * 协作类型：<code>SearchDocumentMapper</code>。
     */
    private final SearchDocumentMapper mapper;

    /**
     * 新增或更新搜索索引。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>TYPES.contains</code>、<code>mapper.upsert</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     *
     * @param document 文档；类型为 <code>SearchableDocument</code>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsert(SearchableDocument document) {
        if (document == null || !TYPES.contains(document.getContentType())
            || document.getContentId() == null || document.getContentId() <= 0
            || document.getDocumentKey() == null || document.getDocumentKey().length() > 120
            || !document.getDocumentKey().equals(document.getContentType() + ":" + document.getContentId())
            || document.getTitle() == null || document.getTitle().isBlank() || document.getTitle().length() > 255
            || document.getRoutePath() == null || !document.getRoutePath().startsWith("/")
            || document.getRoutePath().length() > 500) {
            throw new IllegalArgumentException("Invalid public search document");
        }
        if (document.getSummary() != null && document.getSummary().length() > 1000) {
            document.setSummary(document.getSummary().substring(0, 1000));
        }
        if (document.getSearchableText() == null) document.setSearchableText("");
        if (document.getSearchableText().length() > 100_000) {
            document.setSearchableText(document.getSearchableText().substring(0, 100_000));
        }
        if (document.getSourceUpdatedAt() == null) document.setSourceUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        mapper.upsert(document);
    }

    /**
     * 移除搜索索引。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.remove</code>。
     *
     * @param documentKey 文档键；类型为 <code>String</code>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void remove(String documentKey) {
        if (documentKey != null && !documentKey.isBlank()) mapper.remove(documentKey);
    }

    /**
     * 移除依据内容。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>TYPES.contains</code>、<code>mapper.removeByContent</code>。
     *
     * @param contentType 内容类型或请求声明的 Content-Type；类型为 <code>String</code>
     * @param contentId 内容标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeByContent(String contentType, Long contentId) {
        if (TYPES.contains(contentType) && contentId != null && contentId > 0) mapper.removeByContent(contentType, contentId);
    }

    /**
     * 移除章节列表。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.removeRoutePrefix</code>。
     *
     * @param tutorialSlug 教程固定链接标识；类型为 <code>String</code>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeChapters(String tutorialSlug) {
        if (tutorialSlug != null && tutorialSlug.matches("[a-zA-Z0-9_-]{1,120}")) {
            mapper.removeRoutePrefix("/tutorials/" + tutorialSlug + "/");
        }
    }
}
