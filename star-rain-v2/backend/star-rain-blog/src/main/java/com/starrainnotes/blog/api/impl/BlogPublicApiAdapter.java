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
@Component
public class BlogPublicApiAdapter implements BlogPublicApi {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final BlogPostMapper postMapper;
    private final BlogTopicMapper topicMapper;
    private final BlogViewAssembler assembler;

    public BlogPublicApiAdapter(BlogPostMapper postMapper, BlogTopicMapper topicMapper,
                                BlogViewAssembler assembler) {
        this.postMapper = postMapper;
        this.topicMapper = topicMapper;
        this.assembler = assembler;
    }

    @Override
    public List<BlogPostSummary> latestPublished(int limit) {
        return postMapper.latestPublished(clamp(limit)).stream()
                .map(assembler::toSummary)
                .toList();
    }

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

    @Override
    public List<BlogTopicSummary> publishedTopics() {
        return topicMapper.publishedTopics().stream().map(this::toSummary).toList();
    }

    @Override
    public BlogTopicSummary publishedTopicBySlug(String slug) {
        String normalized = BlogSlugRules.normalize(slug);
        if (normalized == null) {
            return null;
        }
        BlogTopicVO topic = topicMapper.publishedTopicBySlug(normalized);
        return topic == null ? null : toSummary(topic);
    }

    private BlogTopicSummary toSummary(BlogTopicVO topic) {
        return new BlogTopicSummary(topic.getId(), topic.getSlug(), topic.getName(),
                topic.getDescription(), topic.getUpdatedAt());
    }

    private static int clamp(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
