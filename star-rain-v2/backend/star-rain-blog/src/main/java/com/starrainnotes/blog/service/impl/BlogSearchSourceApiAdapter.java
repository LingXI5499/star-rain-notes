package com.starrainnotes.blog.service.impl;

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
@Component
public class BlogSearchSourceApiAdapter implements BlogSearchSourceApi {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final BlogPostMapper postMapper;
    private final BlogViewAssembler assembler;

    public BlogSearchSourceApiAdapter(BlogPostMapper postMapper, BlogViewAssembler assembler) {
        this.postMapper = postMapper;
        this.assembler = assembler;
    }

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

    @Override
    public BlogPostDocument getPublishedDocument(Long postId) {
        BlogPostEntity post = postId == null ? null : postMapper.publishedPostById(postId);
        return post == null ? null : assembler.toDocument(post);
    }
}
