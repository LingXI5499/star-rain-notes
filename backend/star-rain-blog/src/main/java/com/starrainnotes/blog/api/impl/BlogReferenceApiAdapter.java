/*
 * BlogReferenceApiAdapter：博客模块的跨模块接口文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：BlogReferenceApi、BlogPostSummary、BlogPostEntity、BlogPostMapper、BlogViewAssembler。
 * 阅读入口：exists（判断是否存在博客引用接口Adapter）；isPublished（判断是否已发布）；summary（摘要处理入口）。
 */

package com.starrainnotes.blog.api.impl;

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
/**
 * 博客引用接口Adapter，属于博客模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Component
public class BlogReferenceApiAdapter implements BlogReferenceApi {

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
     * 构造BlogReferenceApiAdapter，接收本类使用的依赖或初始化参数。
     *
     * @param postMapper 文章数据映射；类型为 <code>BlogPostMapper</code>
     * @param assembler 组装器；类型为 <code>BlogViewAssembler</code>
     */
    public BlogReferenceApiAdapter(BlogPostMapper postMapper, BlogViewAssembler assembler) {
        this.postMapper = postMapper;
        this.assembler = assembler;
    }

    /**
     * 判断是否存在博客引用接口Adapter。
     * 主要协作调用：<code>postMapper.postById</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean exists(Long postId) {
        return postId != null && postMapper.postById(postId) != null;
    }

    /**
     * 判断是否已发布。
     * 主要协作调用：<code>postMapper.publishedPostById</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean isPublished(Long postId) {
        return postId != null && postMapper.publishedPostById(postId) != null;
    }

    /**
     * 摘要处理入口。
     * 主要协作调用：<code>postMapper.publishedPostById</code>、<code>assembler.toSummary</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>BlogPostSummary</code> 的处理结果
     */
    @Override
    public BlogPostSummary summary(Long postId) {
        BlogPostEntity post = postId == null ? null : postMapper.publishedPostById(postId);
        return post == null ? null : assembler.toSummary(post);
    }
}
