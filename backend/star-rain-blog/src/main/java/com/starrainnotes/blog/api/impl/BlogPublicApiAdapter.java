/*
 * BlogPublicApiAdapter：博客模块的跨模块接口文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：BlogPublicApi、BlogPostSummary、BlogTopicSummary、BlogPostMapper、BlogTopicMapper、BlogViewAssembler、BlogSlugRules。
 * 阅读入口：latestPublished（最新已发布处理入口）；featuredByTopic（按专题读取精选）；publishedTopics（已发布专题列表处理入口）；publishedTopicBySlug（按固定链接标识读取已发布专题）。
 */

package com.starrainnotes.blog.api.impl;

import com.starrainnotes.blog.api.BlogPublicApi;
import com.starrainnotes.blog.api.dto.BlogPostSummary;
import com.starrainnotes.blog.api.dto.BlogTopicSummary;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.mapper.BlogTopicMapper;
import com.starrainnotes.blog.service.BlogViewAssembler;
import com.starrainnotes.blog.utils.BlogSlugRules;
import com.starrainnotes.blog.vo.BlogTopicVO;
import java.util.List;
import org.springframework.stereotype.Component;

/*
 * BlogPublicApi 的适配实现。
 *
 * 把模块内 Entity 转成模块间契约 BlogPostSummary，并在这里收口 limit：
 * 调用方传 0 / 负数 / 巨大值都不该让首页查询失控。
 */
/**
 * 博客公开接口Adapter，属于博客模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Component
public class BlogPublicApiAdapter implements BlogPublicApi {

    /**
     * 上限。
     * 初始值为 <code>5</code>。
     */
    private static final int DEFAULT_LIMIT = 5;
    /**
     * 上限。
     * 初始值为 <code>20</code>。
     */
    private static final int MAX_LIMIT = 20;

    /**
     * 文章数据映射。
     * 协作类型：<code>BlogPostMapper</code>。
     */
    private final BlogPostMapper postMapper;
    /**
     * 专题数据映射。
     * 协作类型：<code>BlogTopicMapper</code>。
     */
    private final BlogTopicMapper topicMapper;
    /**
     * 组装器。
     */
    private final BlogViewAssembler assembler;

    /**
     * 构造BlogPublicApiAdapter，接收本类使用的依赖或初始化参数。
     *
     * @param postMapper 文章数据映射；类型为 <code>BlogPostMapper</code>
     * @param topicMapper 专题数据映射；类型为 <code>BlogTopicMapper</code>
     * @param assembler 组装器；类型为 <code>BlogViewAssembler</code>
     */
    public BlogPublicApiAdapter(BlogPostMapper postMapper, BlogTopicMapper topicMapper,
                                BlogViewAssembler assembler) {
        this.postMapper = postMapper;
        this.topicMapper = topicMapper;
        this.assembler = assembler;
    }

    /**
     * 最新已发布处理入口。
     * 主要协作调用：<code>postMapper.latestPublished</code>。
     *
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<BlogPostSummary> latestPublished(int limit) {
        return postMapper.latestPublished(clamp(limit)).stream()
                .map(assembler::toSummary)
                .toList();
    }

    /**
     * 按专题读取精选。
     * 主要协作调用：<code>postMapper.publishedByTopicSlug</code>。
     *
     * @param topicSlug 专题固定链接标识；类型为 <code>String</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<BlogPostSummary> featuredByTopic(String topicSlug, int limit) {
        String slug = BlogSlugRules.normalize(topicSlug);
        if (slug == null) {
            return List.of();
        }
        return postMapper.publishedByTopicSlug(slug, clamp(limit)).stream()
                .map(assembler::toSummary)
                .toList();
    }

    /**
     * 已发布专题列表处理入口。
     * 主要协作调用：<code>topicMapper.publishedTopics</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<BlogTopicSummary> publishedTopics() {
        return topicMapper.publishedTopics().stream().map(this::toSummary).toList();
    }

    /**
     * 按固定链接标识读取已发布专题。
     * 主要协作调用：<code>topicMapper.publishedTopicBySlug</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>BlogTopicSummary</code> 的处理结果
     */
    @Override
    public BlogTopicSummary publishedTopicBySlug(String slug) {
        String normalized = BlogSlugRules.normalize(slug);
        if (normalized == null) {
            return null;
        }
        BlogTopicVO topic = topicMapper.publishedTopicBySlug(normalized);
        return topic == null ? null : toSummary(topic);
    }

    /**
     * 转换为摘要。
     * 关键处理步骤调用 <code>topic.getId</code>、<code>topic.getSlug</code>、<code>topic.getName</code>、<code>topic.getDescription</code>。
     *
     * @param topic 专题；类型为 <code>BlogTopicVO</code>
     * @return 类型为 <code>BlogTopicSummary</code> 的处理结果
     */
    private BlogTopicSummary toSummary(BlogTopicVO topic) {
        return new BlogTopicSummary(topic.getId(), topic.getSlug(), topic.getName(),
                topic.getDescription(), topic.getUpdatedAt());
    }

    /**
     * 限制范围处理入口。
     * 关键处理步骤调用 <code>Math.min</code>。
     *
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    private static int clamp(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
