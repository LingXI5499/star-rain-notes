/*
 * SearchRebuildServiceImpl：搜索模块的业务实现文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：BlogSearchSourceApi、BlogDocumentPage、BlogPostDocument、PageResult、EnglishSearchSourceApi、EnglishSearchTypes、EnglishSearchDocument。
 * 阅读入口：rebuild（重建搜索Rebuild）；rebuildAll（重建全部）；rebuildType（重建类型）；indexBlog（索引博客处理入口）。
 */

package com.starrainnotes.search.service.impl;

import com.starrainnotes.blog.api.BlogSearchSourceApi;
import com.starrainnotes.blog.api.dto.BlogDocumentPage;
import com.starrainnotes.blog.api.dto.BlogPostDocument;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.english.api.EnglishSearchSourceApi;
import com.starrainnotes.english.api.EnglishSearchTypes;
import com.starrainnotes.english.api.dto.EnglishSearchDocument;
import com.starrainnotes.portfolio.api.PortfolioPublicApi;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import com.starrainnotes.profile.api.ProfilePublicApi;
import com.starrainnotes.profile.api.dto.ProfilePublishedDocument;
import com.starrainnotes.search.api.SearchIndexApi;
import com.starrainnotes.search.api.dto.SearchableDocument;
import com.starrainnotes.search.service.SearchIndexService;
import com.starrainnotes.search.service.SearchRebuildService;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.search.exception.SearchTypeInvalidException;
import com.starrainnotes.search.utils.SearchTextExtractor;
import com.starrainnotes.tutorial.content.api.TutorialChapterDocument;
import com.starrainnotes.tutorial.content.api.TutorialDocumentSourceApi;
import com.starrainnotes.tutorial.content.api.TutorialPublishedDocument;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/**
 * 搜索Rebuild业务服务实现，属于搜索模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class SearchRebuildServiceImpl implements SearchRebuildService {
    /**
     * 批量。
     * 初始值为 <code>100</code>。
     */
    private static final int BATCH = 100;
    /**
     * blogs。
     * 协作类型：<code>BlogSearchSourceApi</code>。
     */
    private final BlogSearchSourceApi blogs;
    /**
     * tutorials。
     * 协作类型：<code>TutorialDocumentSourceApi</code>。
     */
    private final TutorialDocumentSourceApi tutorials;
    /**
     * 作品列表。
     * 协作类型：<code>PortfolioPublicApi</code>。
     */
    private final PortfolioPublicApi works;
    /**
     * profiles。
     * 协作类型：<code>ProfilePublicApi</code>。
     */
    private final ProfilePublicApi profiles;
    /**
     * 索引。
     * 协作类型：<code>SearchIndexApi</code>。
     */
    private final SearchIndexApi index;
    /**
     * 数据映射。
     * 协作类型：<code>SearchDocumentMapper</code>。
     */
    private final SearchDocumentMapper mapper;
    /**
     * extractor。
     */
    private final SearchTextExtractor extractor;
    /**
     * 英语。
     * 协作类型：<code>EnglishSearchSourceApi</code>。
     */
    private final EnglishSearchSourceApi english;

    /**
     * 重建搜索Rebuild。
     * 主要协作调用：<code>mapper.activeCount</code>。
     * 实现中的显式失败类型包括 <code>SearchTypeInvalidException</code>；具体触发条件见对应分支。
     *
     * @param type 类型；类型为 <code>String</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    @Override
    public long rebuild(String type) {
        if (type == null || type.isBlank()) rebuildAll();
        else {
            if (!"ENGLISH".equals(type) && !SearchIndexService.TYPES.contains(type)) throw new SearchTypeInvalidException();
            rebuildType(type);
        }
        return mapper.activeCount();
    }

    /**
     * 重建全部。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>rebuildTutorials</code>、<code>rebuildBlogs</code>、<code>rebuildWorks</code>、<code>rebuildProfile</code>。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rebuildAll() {
        rebuildTutorials();
        rebuildBlogs();
        rebuildWorks();
        rebuildProfile();
        EnglishSearchTypes.ALL.forEach(this::rebuildEnglish);
    }

    /**
     * 重建类型。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>EnglishSearchTypes.ALL.forEach</code>、<code>SearchIndexService.TYPES.contains</code>、<code>EnglishSearchTypes.ALL.contains</code>、<code>rebuildEnglish</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     *
     * @param type 类型；类型为 <code>String</code>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rebuildType(String type) {
        if ("ENGLISH".equals(type)) {
            EnglishSearchTypes.ALL.forEach(this::rebuildEnglish);
            return;
        }
        if (!SearchIndexService.TYPES.contains(type)) throw new IllegalArgumentException("Invalid search type");
        if (EnglishSearchTypes.ALL.contains(type)) {
            rebuildEnglish(type);
            return;
        }
        switch (type) {
            case "TUTORIAL", "CHAPTER" -> rebuildTutorials();
            case "BLOG" -> rebuildBlogs();
            case "PORTFOLIO" -> rebuildWorks();
            case "PROFILE" -> rebuildProfile();
            default -> throw new IllegalArgumentException("Invalid search type");
        }
    }

    /**
     * 索引博客处理入口。
     * 主要协作调用：<code>index.upsert</code>。
     *
     * @param post 文章；类型为 <code>BlogPostDocument</code>
     */
    @Override
    public void indexBlog(BlogPostDocument post) {
        index.upsert(document("BLOG", post.getId(), post.getTitle(), post.getSummary(),
            post.getBodyMarkdown(), "/blog/posts/" + post.getSlug(), post.getPublishedAt(), post.getUpdatedAt()));
    }

    /**
     * 索引作品处理入口。
     * 主要协作调用：<code>index.upsert</code>。
     *
     * @param work 作品；类型为 <code>PortfolioPublishedWork</code>
     */
    @Override
    public void indexWork(PortfolioPublishedWork work) {
        index.upsert(document("PORTFOLIO", work.getId(), work.getTitle(), work.getSummary(),
            work.getSearchableText() == null ? work.getBodyMarkdown() : work.getSearchableText(), "/portfolio/" + work.getSlug(), work.getPublishedAt(), work.getUpdatedAt()));
    }

    /**
     * 重建Blogs。
     * 主要协作调用：<code>blogs.listPublishedDocuments</code>。
     */
    private void rebuildBlogs() {
        Set<String> seen = new HashSet<>();
        Long cursor = null;
        do {
            BlogDocumentPage result = blogs.listPublishedDocuments(cursor, BATCH);
            for (BlogPostDocument post : result.getItems()) {
                indexBlog(post);
                seen.add(key("BLOG", post.getId()));
            }
            cursor = result.getNextCursor();
        } while (cursor != null);
        removeStale("BLOG", seen);
    }

    /**
     * 同步英语。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>english.findPublished</code>、<code>index.removeByContent</code>。
     * 实现中的显式失败类型包括 <code>SearchTypeInvalidException</code>；具体触发条件见对应分支。
     *
     * @param contentType 内容类型或请求声明的 Content-Type；类型为 <code>String</code>
     * @param contentId 内容标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncEnglish(String contentType, Long contentId) {
        if (!EnglishSearchTypes.ALL.contains(contentType)) throw new SearchTypeInvalidException();
        EnglishSearchDocument source = english.findPublished(contentType, contentId);
        if (source == null) index.removeByContent(contentType, contentId);
        else indexEnglish(source);
    }

    /**
     * 重建英语。
     * 主要协作调用：<code>english.page</code>。
     *
     * @param type 类型；类型为 <code>String</code>
     */
    private void rebuildEnglish(String type) {
        Set<String> seen = new HashSet<>();
        Long cursor = null;
        while (true) {
            List<EnglishSearchDocument> batch = english.page(type, cursor, BATCH);
            for (EnglishSearchDocument source : batch) {
                indexEnglish(source);
                seen.add(key(type, source.getId()));
            }
            if (batch.size() < BATCH) break;
            cursor = batch.getLast().getId();
        }
        removeStale(type, seen);
    }

    /**
     * 索引英语处理入口。
     * 主要协作调用：<code>index.upsert</code>。
     *
     * @param source 来源；类型为 <code>EnglishSearchDocument</code>
     */
    private void indexEnglish(EnglishSearchDocument source) {
        index.upsert(document(source.getContentType(), source.getId(), source.getTitle(), source.getSummary(),
            source.getSearchableText(), source.getRoutePath(), source.getPublishedAt(), source.getUpdatedAt()));
    }

    /**
     * 重建Tutorials。
     * 主要协作调用：<code>tutorials.page</code>、<code>index.upsert</code>。
     */
    private void rebuildTutorials() {
        Set<String> seenTutorials = new HashSet<>();
        Set<String> seenChapters = new HashSet<>();
        for (int page = 1; ; page++) {
            PageResult<TutorialPublishedDocument> result = tutorials.page(page, BATCH);
            for (TutorialPublishedDocument detail : result.getItems()) {
                index.upsert(document("TUTORIAL", detail.getId(), detail.getTitle(), detail.getSummary(), "",
                    "/tutorials/" + detail.getSlug(), detail.getPublishedAt(), detail.getPublishedAt()));
                seenTutorials.add(key("TUTORIAL", detail.getId()));
                for (TutorialChapterDocument chapter : detail.getChapters()) {
                    index.upsert(document("CHAPTER", chapter.getId(), chapter.getTitle(), chapter.getSummary(),
                        chapter.getSearchableText(), "/tutorials/" + detail.getSlug() + "/" + chapter.getSlug(),
                        detail.getPublishedAt(), detail.getPublishedAt()));
                    seenChapters.add(key("CHAPTER", chapter.getId()));
                }
            }
            if ((long) page * BATCH >= result.getTotal()) break;
        }
        removeStale("TUTORIAL", seenTutorials);
        removeStale("CHAPTER", seenChapters);
    }

    /**
     * 重建作品列表。
     * 主要协作调用：<code>works.publishedWorks</code>。
     */
    private void rebuildWorks() {
        Set<String> seen = new HashSet<>();
        for (int page = 1; ; page++) {
            PageResult<PortfolioPublishedWork> result = works.publishedWorks(page, BATCH);
            for (PortfolioPublishedWork item : result.getItems()) {
                indexWork(item);
                seen.add(key("PORTFOLIO", item.getId()));
            }
            if ((long) page * BATCH >= result.getTotal()) break;
        }
        removeStale("PORTFOLIO", seen);
    }

    /**
     * 重建资料。
     * 主要协作调用：<code>profiles.publishedDocument</code>、<code>index.upsert</code>。
     */
    private void rebuildProfile() {
        ProfilePublishedDocument profile = profiles.publishedDocument();
        Set<String> seen = new HashSet<>();
        if (profile != null) {
            index.upsert(document("PROFILE", profile.getId(), profile.getTitle(), profile.getSummary(),
                profile.getBodyMarkdown(),
                "/about", null, LocalDateTime.now(ZoneOffset.UTC)));
            seen.add(key("PROFILE", profile.getId()));
        }
        removeStale("PROFILE", seen);
    }

    /**
     * 移除Stale。
     * 主要协作调用：<code>mapper.activeKeys</code>、<code>index.remove</code>。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param seen seen；类型为 <code>Set&lt;String&gt;</code>
     */
    private void removeStale(String type, Set<String> seen) {
        for (String key : mapper.activeKeys(type)) {
            if (!seen.contains(key)) index.remove(key);
        }
    }

    /**
     * 文档处理入口。
     * 主要协作调用：<code>extractor.plainText</code>。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param title 标题；类型为 <code>String</code>
     * @param summary 摘要；类型为 <code>String</code>
     * @param markdown Markdown；类型为 <code>String</code>
     * @param route 路由；类型为 <code>String</code>
     * @param publishedAt 已发布时间；类型为 <code>LocalDateTime</code>
     * @param updatedAt 更新时间；类型为 <code>LocalDateTime</code>
     * @return 类型为 <code>SearchableDocument</code> 的处理结果
     */
    private SearchableDocument document(String type, Long id, String title, String summary,
        String markdown, String route, LocalDateTime publishedAt, LocalDateTime updatedAt) {
        SearchableDocument result = new SearchableDocument();
        result.setContentType(type);
        result.setContentId(id);
        result.setDocumentKey(key(type, id));
        result.setTitle(title);
        result.setSummary(summary);
        result.setSearchableText(extractor.plainText(markdown));
        result.setRoutePath(route);
        result.setPublishedAt(publishedAt);
        result.setSourceUpdatedAt(updatedAt == null ? LocalDateTime.now(ZoneOffset.UTC) : updatedAt);
        return result;
    }

    /**
     * 键处理入口。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String key(String type, Long id) { return type + ":" + id; }
}
