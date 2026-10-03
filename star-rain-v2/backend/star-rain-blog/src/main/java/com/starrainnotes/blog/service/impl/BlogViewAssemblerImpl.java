package com.starrainnotes.blog.service.impl;

import com.starrainnotes.blog.api.dto.BlogPostDocument;
import com.starrainnotes.blog.api.dto.BlogPostSummary;
import com.starrainnotes.blog.dto.BlogPostTagRow;
import com.starrainnotes.blog.dto.BlogPostTopicRow;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.mapper.BlogTopicMapper;
import com.starrainnotes.blog.service.BlogViewAssembler;
import com.starrainnotes.blog.utils.BlogContentMediaParser;
import com.starrainnotes.blog.utils.BlogTextRules;
import com.starrainnotes.blog.vo.BlogPostAdminDetailVO;
import com.starrainnotes.blog.vo.BlogPostAdminVO;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import com.starrainnotes.blog.vo.BlogPostPublicVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.media.api.MediaAssetApi;
import com.starrainnotes.media.api.dto.MediaAssetSummary;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/*
 * 视图组装的唯一实现。
 *
 * 不掺业务规则：这里只做“实体 + 分类 + 封面地址 → VO”，
 * 状态判断与权限校验留在各自的 Service，避免规则散落两处后互相矛盾。
 */
@Component
public class BlogViewAssemblerImpl implements BlogViewAssembler {

    private final BlogTagMapper tagMapper;
    private final BlogTopicMapper topicMapper;
    private final MediaAssetApi mediaAssetApi;

    public BlogViewAssemblerImpl(BlogTagMapper tagMapper,
                                BlogTopicMapper topicMapper,
                                MediaAssetApi mediaAssetApi) {
        this.tagMapper = tagMapper;
        this.topicMapper = topicMapper;
        this.mediaAssetApi = mediaAssetApi;
    }

    @Override
    public BlogTagVO toTagVO(BlogTagEntity entity) {
        if (entity == null) {
            return null;
        }
        return BlogTagVO.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public List<BlogTagVO> tagsOf(Long postId) {
        return tagMapper.tagsByPostId(postId);
    }

    @Override
    public List<BlogTopicVO> topicsOf(Long postId) {
        return topicMapper.topicsByPostId(postId);
    }

    @Override
    public String coverUrl(Long mediaAssetId) {
        if (mediaAssetId == null) {
            return null;
        }
        MediaAssetSummary summary = mediaAssetApi.get(mediaAssetId);
        // 被归档的媒体仍可能被历史文章引用，此时不给地址，让前端显示占位图
        return summary == null ? null : summary.getContentUrl();
    }

    @Override
    public BlogPostAdminVO toAdminVO(BlogPostEntity post) {
        return adminVO(post, tagsOf(post.getId()), topicsOf(post.getId()));
    }

    @Override
    public List<BlogPostAdminVO> toAdminVOs(List<BlogPostEntity> posts) {
        if (posts == null || posts.isEmpty()) {
            return List.of();
        }
        List<Long> postIds = posts.stream().map(BlogPostEntity::getId).toList();
        Map<Long, List<BlogTagVO>> tags = groupTags(tagMapper.tagsByPostIds(postIds));
        Map<Long, List<BlogTopicVO>> topics = groupTopics(topicMapper.topicsByPostIds(postIds));
        List<BlogPostAdminVO> result = new ArrayList<>(posts.size());
        for (BlogPostEntity post : posts) {
            result.add(adminVO(post, tags.getOrDefault(post.getId(), List.of()),
                    topics.getOrDefault(post.getId(), List.of())));
        }
        return result;
    }

    @Override
    public BlogPostAdminDetailVO toAdminDetailVO(BlogPostEntity post) {
        return BlogPostAdminDetailVO.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .summary(post.getSummary())
                .bodyMarkdown(post.getBodyMarkdown())
                .coverMediaAssetId(post.getCoverMediaAssetId())
                .coverUrl(coverUrl(post.getCoverMediaAssetId()))
                .contentMediaAssetIds(BlogContentMediaParser.extractMediaAssetIds(post.getBodyMarkdown()))
                .status(post.getStatus())
                .publishedAt(post.getPublishedAt())
                .withdrawnAt(post.getWithdrawnAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .createdByAccountId(post.getCreatedByAccountId())
                .updatedByAccountId(post.getUpdatedByAccountId())
                .tags(tagsOf(post.getId()))
                .topics(topicsOf(post.getId()))
                .hasDisabledTags(tagMapper.countDisabledTagsByPostId(post.getId()) > 0)
                .build();
    }

    @Override
    public BlogPostPublicVO toPublicVO(BlogPostEntity post) {
        return publicVO(post, tagsOf(post.getId()), topicsOf(post.getId()));
    }

    @Override
    public List<BlogPostPublicVO> toPublicVOs(List<BlogPostEntity> posts) {
        if (posts == null || posts.isEmpty()) {
            return List.of();
        }
        List<Long> postIds = posts.stream().map(BlogPostEntity::getId).toList();
        Map<Long, List<BlogTagVO>> tags = groupTags(tagMapper.tagsByPostIds(postIds));
        Map<Long, List<BlogTopicVO>> topics = groupTopics(topicMapper.topicsByPostIds(postIds));
        List<BlogPostPublicVO> result = new ArrayList<>(posts.size());
        for (BlogPostEntity post : posts) {
            result.add(publicVO(post, tags.getOrDefault(post.getId(), List.of()),
                    topics.getOrDefault(post.getId(), List.of())));
        }
        return result;
    }

    @Override
    public BlogPostPublicDetailVO toPublicDetailVO(BlogPostEntity post) {
        return BlogPostPublicDetailVO.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .summary(post.getSummary())
                .bodyMarkdown(post.getBodyMarkdown())
                .coverMediaAssetId(post.getCoverMediaAssetId())
                .coverUrl(coverUrl(post.getCoverMediaAssetId()))
                .publishedAt(post.getPublishedAt())
                .updatedAt(post.getUpdatedAt())
                .tags(tagsOf(post.getId()))
                .topics(topicsOf(post.getId()))
                .build();
    }

    @Override
    public BlogPostSummary toSummary(BlogPostEntity post) {
        List<BlogTagVO> tags = tagsOf(post.getId());
        List<BlogTopicVO> topics = topicsOf(post.getId());
        return BlogPostSummary.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .summary(post.getSummary())
                .coverUrl(coverUrl(post.getCoverMediaAssetId()))
                .publishedAt(post.getPublishedAt())
                .tagNames(tags.stream().map(BlogTagVO::getName).toList())
                .topicNames(topics.stream().map(BlogTopicVO::getName).toList())
                .build();
    }

    @Override
    public BlogPostDocument toDocument(BlogPostEntity post) {
        List<BlogTagVO> tags = tagsOf(post.getId());
        List<BlogTopicVO> topics = topicsOf(post.getId());
        return BlogPostDocument.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .summary(post.getSummary())
                .bodyMarkdown(post.getBodyMarkdown())
                .coverUrl(coverUrl(post.getCoverMediaAssetId()))
                .publishedAt(post.getPublishedAt())
                .updatedAt(post.getUpdatedAt())
                .tagSlugs(tags.stream().map(BlogTagVO::getSlug).toList())
                .topicSlugs(topics.stream().map(BlogTopicVO::getSlug).toList())
                .build();
    }

    private BlogPostAdminVO adminVO(BlogPostEntity post, List<BlogTagVO> tags, List<BlogTopicVO> topics) {
        return BlogPostAdminVO.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .summary(post.getSummary())
                .status(post.getStatus())
                .coverMediaAssetId(post.getCoverMediaAssetId())
                .coverUrl(coverUrl(post.getCoverMediaAssetId()))
                .hasBody(BlogTextRules.hasText(post.getBodyMarkdown()))
                .publishedAt(post.getPublishedAt())
                .withdrawnAt(post.getWithdrawnAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .tags(tags)
                .topics(topics)
                .build();
    }

    private BlogPostPublicVO publicVO(BlogPostEntity post, List<BlogTagVO> tags, List<BlogTopicVO> topics) {
        return BlogPostPublicVO.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .summary(post.getSummary())
                .coverMediaAssetId(post.getCoverMediaAssetId())
                .coverUrl(coverUrl(post.getCoverMediaAssetId()))
                .publishedAt(post.getPublishedAt())
                .updatedAt(post.getUpdatedAt())
                .tags(tags)
                .topics(topics)
                .build();
    }

    private static Map<Long, List<BlogTagVO>> groupTags(List<BlogPostTagRow> rows) {
        Map<Long, List<BlogTagVO>> grouped = new LinkedHashMap<>();
        for (BlogPostTagRow row : rows) {
            grouped.computeIfAbsent(row.getPostId(), key -> new ArrayList<>())
                    .add(BlogTagVO.builder()
                            .id(row.getTagId())
                            .slug(row.getSlug())
                            .name(row.getName())
                            .build());
        }
        return grouped;
    }

    private static Map<Long, List<BlogTopicVO>> groupTopics(List<BlogPostTopicRow> rows) {
        Map<Long, List<BlogTopicVO>> grouped = new LinkedHashMap<>();
        for (BlogPostTopicRow row : rows) {
            grouped.computeIfAbsent(row.getPostId(), key -> new ArrayList<>())
                    .add(BlogTopicVO.builder()
                            .id(row.getTopicId())
                            .slug(row.getSlug())
                            .name(row.getName())
                            .build());
        }
        return grouped;
    }
}
