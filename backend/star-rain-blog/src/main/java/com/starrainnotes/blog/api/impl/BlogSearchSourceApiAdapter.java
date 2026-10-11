/*
 * BlogSearchSourceApiAdapter：博客模块的跨模块接口文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：BlogSearchSourceApi、BlogDocumentPage、BlogPostDocument、BlogPostEntity、BlogPostMapper、BlogViewAssembler。
 * 阅读入口：listPublishedDocuments（列出已发布Documents）；getPublishedDocument（读取已发布文档）；getPublishedDocumentBySlug（读取已发布文档依据固定链接标识）。
 */

package com.starrainnotes.blog.api.impl;

import com.starrainnotes.blog.api.BlogSearchSourceApi;
import com.starrainnotes.blog.api.dto.BlogDocumentPage;
import com.starrainnotes.blog.api.dto.BlogPostDocument;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.service.BlogViewAssembler;
import java.util.List;
import org.springframework.stereotype.Component;

/*
 * BlogSearchSourceApi 的适配实现。
 *
 * 游标是最后一条的 ID：增量重建索引时即使中途有新文章发布，
 * 也不会像 offset 分页那样漏读或重复读。
 */
/**
 * 博客搜索来源接口Adapter，属于博客模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Component
public class BlogSearchSourceApiAdapter implements BlogSearchSourceApi {

    /**
     * 上限。
     * 初始值为 <code>50</code>。
     */
    private static final int DEFAULT_LIMIT = 50;
    /**
     * 上限。
     * 初始值为 <code>200</code>。
     */
    private static final int MAX_LIMIT = 200;

    /**
     * 文章数据映射。
     * 协作类型：<code>BlogPostMapper</code>。
     */
    private final BlogPostMapper postMapper;
    /**
     * 组装器。
     */
    private final BlogViewAssembler assembler;

    /**
     * 构造BlogSearchSourceApiAdapter，接收本类使用的依赖或初始化参数。
     *
     * @param postMapper 文章数据映射；类型为 <code>BlogPostMapper</code>
     * @param assembler 组装器；类型为 <code>BlogViewAssembler</code>
     */
    public BlogSearchSourceApiAdapter(BlogPostMapper postMapper, BlogViewAssembler assembler) {
        this.postMapper = postMapper;
        this.assembler = assembler;
    }

    /**
     * 列出已发布Documents。
     * 主要协作调用：<code>postMapper.publishedAfterCursor</code>。
     *
     * @param cursor cursor；类型为 <code>Long</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 类型为 <code>BlogDocumentPage</code> 的处理结果
     */
    @Override
    public BlogDocumentPage listPublishedDocuments(Long cursor, int limit) {
        int size = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        List<BlogPostEntity> rows = postMapper.publishedAfterCursor(cursor, size);
        List<BlogPostDocument> items = rows.stream().map(assembler::toDocument).toList();
        // 取满一页才给下一页游标：不足一页说明已经到末尾，避免调用方多跑一次空查询
        Long nextCursor = rows.size() < size ? null : rows.get(rows.size() - 1).getId();
        return BlogDocumentPage.builder()
                .items(items)
                .nextCursor(nextCursor)
                .build();
    }

    /**
     * 读取已发布文档。
     * 主要协作调用：<code>postMapper.publishedPostById</code>、<code>assembler.toDocument</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>BlogPostDocument</code> 的处理结果
     */
    @Override
    public BlogPostDocument getPublishedDocument(Long postId) {
        BlogPostEntity post = postId == null ? null : postMapper.publishedPostById(postId);
        return post == null ? null : assembler.toDocument(post);
    }

    /**
     * 读取已发布文档依据固定链接标识。
     * 主要协作调用：<code>postMapper.publishedPostBySlug</code>、<code>assembler.toDocument</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>BlogPostDocument</code> 的处理结果
     */
    @Override
    public BlogPostDocument getPublishedDocumentBySlug(String slug) {
        BlogPostEntity post = slug == null ? null : postMapper.publishedPostBySlug(slug);
        return post == null ? null : assembler.toDocument(post);
    }
}
