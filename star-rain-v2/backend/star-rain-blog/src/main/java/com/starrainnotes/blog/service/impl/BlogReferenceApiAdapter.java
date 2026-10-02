package com.starrainnotes.blog.service.impl;

import com.starrainnotes.blog.api.BlogReferenceApi;
import com.starrainnotes.blog.api.dto.BlogPostSummary;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.service.BlogViewAssembler;
import org.springframework.stereotype.Component;

/*
 * BlogReferenceApi 的适配实现。
 *
 * exists 与 isPublished 分开，是因为调用方要区分两种用途：
 * “链接指向的文章还在不在”（草稿也算存在）与“能不能放进公开精选区”（必须已发布）。
 *
 * summary 只在已发布时返回值：跨模块调用方拿它几乎都是为了公开展示，
 * 若草稿也返回摘要，Profile 精选区就可能把未发布内容渲染出去。
 */
@Component
public class BlogReferenceApiAdapter implements BlogReferenceApi {

    private final BlogPostMapper postMapper;
    private final BlogViewAssembler assembler;

    public BlogReferenceApiAdapter(BlogPostMapper postMapper, BlogViewAssembler assembler) {
        this.postMapper = postMapper;
        this.assembler = assembler;
    }

    @Override
    public boolean exists(Long postId) {
        return postId != null && postMapper.postById(postId) != null;
    }

    @Override
    public boolean isPublished(Long postId) {
        return postId != null && postMapper.publishedPostById(postId) != null;
    }

    @Override
    public BlogPostSummary summary(Long postId) {
        BlogPostEntity post = postId == null ? null : postMapper.publishedPostById(postId);
        return post == null ? null : assembler.toSummary(post);
    }
}
