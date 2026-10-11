/*
 * BlogViewAssemblerImpl：博客模块的业务实现文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：BlogPostDocument、BlogPostSummary、BlogPostTagRow、BlogPostTopicRow、BlogPostEntity、BlogTagEntity、BlogTagMapper。
 * 阅读入口：toTagVO（转换为标签响应对象）；tagsOf（标签列表所属处理入口）；topicsOf（专题列表所属处理入口）；coverUrl（封面地址处理入口）。
 */

package com.starrainnotes.blog.service.impl;

import com.starrainnotes.blog.api.dto.BlogPostDocument;
import com.starrainnotes.blog.api.dto.BlogPostSummary;
import com.starrainnotes.blog.dto.BlogPostTagRow;
import com.starrainnotes.blog.dto.BlogPostTopicRow;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.mapper.BlogTopicMapper;
import com.starrainnotes.blog.mapper.BlogPostMapper;
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
/**
 * 博客View组装器实现，属于博客模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Component
public class BlogViewAssemblerImpl implements BlogViewAssembler {

    /**
     * 标签数据映射。
     * 协作类型：<code>BlogTagMapper</code>。
     */
    private final BlogTagMapper tagMapper;
    /**
     * 专题数据映射。
     * 协作类型：<code>BlogTopicMapper</code>。
     */
    private final BlogTopicMapper topicMapper;
    /**
     * 媒体资产接口。
     * 协作类型：<code>MediaAssetApi</code>。
     */
    private final MediaAssetApi mediaAssetApi;
    /**
     * 文章数据映射。
     * 协作类型：<code>BlogPostMapper</code>。
     */
    private final BlogPostMapper postMapper;

    /**
     * 构造BlogViewAssemblerImpl，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>this</code>。
     *
     * @param tagMapper 标签数据映射；类型为 <code>BlogTagMapper</code>
     * @param topicMapper 专题数据映射；类型为 <code>BlogTopicMapper</code>
     * @param mediaAssetApi 媒体资产接口；类型为 <code>MediaAssetApi</code>
     */
    public BlogViewAssemblerImpl(BlogTagMapper tagMapper,
                                BlogTopicMapper topicMapper,
                                MediaAssetApi mediaAssetApi) {
        this(tagMapper, topicMapper, mediaAssetApi, null);
    }

    /**
     * 构造BlogViewAssemblerImpl，接收本类使用的依赖或初始化参数。
     *
     * @param tagMapper 标签数据映射；类型为 <code>BlogTagMapper</code>
     * @param topicMapper 专题数据映射；类型为 <code>BlogTopicMapper</code>
     * @param mediaAssetApi 媒体资产接口；类型为 <code>MediaAssetApi</code>
     * @param postMapper 文章数据映射；类型为 <code>BlogPostMapper</code>
     */
    @org.springframework.beans.factory.annotation.Autowired
    public BlogViewAssemblerImpl(BlogTagMapper tagMapper, BlogTopicMapper topicMapper,
                                 MediaAssetApi mediaAssetApi, BlogPostMapper postMapper) {
        this.tagMapper = tagMapper;
        this.topicMapper = topicMapper;
        this.mediaAssetApi = mediaAssetApi;
        this.postMapper = postMapper;
    }

    /**
     * 摘要处理入口。
     * 主要协作调用：<code>postMapper.postById</code>。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String summary(BlogPostEntity post) {
        String body = post.getBodyMarkdown();
        // Legacy TeX summaries need their original body; ordinary lists never fetch it.
        if (body == null && postMapper != null && BlogTextRules.hasMathMarkup(post.getSummary())) {
            BlogPostEntity detail = postMapper.postById(post.getId());
            body = detail == null ? null : detail.getBodyMarkdown();
        }
        return BlogTextRules.publicSummary(post.getSummary(), body);
    }

    /**
     * 转换为标签响应对象。
     * 关键处理步骤调用 <code>BlogTagVO.builder</code>、<code>entity.getId</code>、<code>entity.getSlug</code>、<code>entity.getName</code>。
     *
     * @param entity 数据库映射实体；类型为 <code>BlogTagEntity</code>
     * @return 类型为 <code>BlogTagVO</code> 的处理结果
     */
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

    /**
     * 标签列表所属处理入口。
     * 主要协作调用：<code>tagMapper.tagsByPostId</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<BlogTagVO> tagsOf(Long postId) {
        return tagMapper.tagsByPostId(postId);
    }

    /**
     * 专题列表所属处理入口。
     * 主要协作调用：<code>topicMapper.topicsByPostId</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<BlogTopicVO> topicsOf(Long postId) {
        return topicMapper.topicsByPostId(postId);
    }

    /**
     * 封面地址处理入口。
     * 主要协作调用：<code>mediaAssetApi.get</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String coverUrl(Long mediaAssetId) {
        if (mediaAssetId == null) {
            return null;
        }
        MediaAssetSummary summary = mediaAssetApi.get(mediaAssetId);
        // 被归档的媒体仍可能被历史文章引用，此时不给地址，让前端显示占位图
        return summary == null ? null : summary.getContentUrl();
    }

    /**
     * 转换为后台响应对象。
     * 关键处理步骤调用 <code>adminVO</code>、<code>tagsOf</code>、<code>post.getId</code>、<code>topicsOf</code>。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @return 类型为 <code>BlogPostAdminVO</code> 的处理结果
     */
    @Override
    public BlogPostAdminVO toAdminVO(BlogPostEntity post) {
        return adminVO(post, tagsOf(post.getId()), topicsOf(post.getId()));
    }

    /**
     * 转换为后台VOs。
     * 主要协作调用：<code>tagMapper.tagsByPostIds</code>、<code>topicMapper.topicsByPostIds</code>。
     *
     * @param posts 文章列表；类型为 <code>List&lt;BlogPostEntity&gt;</code>
     * @return 本次处理得到的条目集合
     */
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

    /**
     * 转换为后台详情响应对象。
     * 主要协作调用：<code>tagMapper.countDisabledTagsByPostId</code>。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @return 类型为 <code>BlogPostAdminDetailVO</code> 的处理结果
     */
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

    /**
     * 转换为公开响应对象。
     * 关键处理步骤调用 <code>publicVO</code>、<code>tagsOf</code>、<code>post.getId</code>、<code>publicTopicsOf</code>。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @return 类型为 <code>BlogPostPublicVO</code> 的处理结果
     */
    @Override
    public BlogPostPublicVO toPublicVO(BlogPostEntity post) {
        return publicVO(post, tagsOf(post.getId()), publicTopicsOf(post.getId()));
    }

    /**
     * 转换为公开VOs。
     * 主要协作调用：<code>tagMapper.tagsByPostIds</code>、<code>topicMapper.publicTopicsByPostIds</code>。
     *
     * @param posts 文章列表；类型为 <code>List&lt;BlogPostEntity&gt;</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<BlogPostPublicVO> toPublicVOs(List<BlogPostEntity> posts) {
        if (posts == null || posts.isEmpty()) {
            return List.of();
        }
        List<Long> postIds = posts.stream().map(BlogPostEntity::getId).toList();
        Map<Long, List<BlogTagVO>> tags = groupTags(tagMapper.tagsByPostIds(postIds));
        Map<Long, List<BlogTopicVO>> topics = groupTopics(topicMapper.publicTopicsByPostIds(postIds));
        List<BlogPostPublicVO> result = new ArrayList<>(posts.size());
        for (BlogPostEntity post : posts) {
            result.add(publicVO(post, tags.getOrDefault(post.getId(), List.of()),
                    topics.getOrDefault(post.getId(), List.of())));
        }
        return result;
    }

    /**
     * 转换为公开详情响应对象。
     * 关键处理步骤调用 <code>BlogPostPublicDetailVO.builder</code>、<code>post.getId</code>、<code>post.getSlug</code>、<code>post.getTitle</code>。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @return 类型为 <code>BlogPostPublicDetailVO</code> 的处理结果
     */
    @Override
    public BlogPostPublicDetailVO toPublicDetailVO(BlogPostEntity post) {
        return BlogPostPublicDetailVO.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .summary(summary(post))
                .bodyMarkdown(post.getBodyMarkdown())
                .coverMediaAssetId(post.getCoverMediaAssetId())
                .coverUrl(coverUrl(post.getCoverMediaAssetId()))
                .publishedAt(post.getPublishedAt())
                .updatedAt(post.getUpdatedAt())
                .tags(tagsOf(post.getId()))
                .topics(publicTopicsOf(post.getId()))
                .build();
    }

    /**
     * 转换为摘要。
     * 关键处理步骤调用 <code>tagsOf</code>、<code>post.getId</code>、<code>topicsOf</code>、<code>BlogPostSummary.builder</code>。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @return 类型为 <code>BlogPostSummary</code> 的处理结果
     */
    @Override
    public BlogPostSummary toSummary(BlogPostEntity post) {
        List<BlogTagVO> tags = tagsOf(post.getId());
        List<BlogTopicVO> topics = topicsOf(post.getId());
        return BlogPostSummary.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .summary(summary(post))
                .coverUrl(coverUrl(post.getCoverMediaAssetId()))
                .publishedAt(post.getPublishedAt())
                .tagNames(tags.stream().map(BlogTagVO::getName).toList())
                .topicNames(topics.stream().map(BlogTopicVO::getName).toList())
                .build();
    }

    /**
     * 转换为文档。
     * 关键处理步骤调用 <code>tagsOf</code>、<code>post.getId</code>、<code>topicsOf</code>、<code>BlogPostDocument.builder</code>。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @return 类型为 <code>BlogPostDocument</code> 的处理结果
     */
    @Override
    public BlogPostDocument toDocument(BlogPostEntity post) {
        List<BlogTagVO> tags = tagsOf(post.getId());
        List<BlogTopicVO> topics = topicsOf(post.getId());
        return BlogPostDocument.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .summary(summary(post))
                .bodyMarkdown(post.getBodyMarkdown())
                .coverUrl(coverUrl(post.getCoverMediaAssetId()))
                .publishedAt(post.getPublishedAt())
                .updatedAt(post.getUpdatedAt())
                .tagSlugs(tags.stream().map(BlogTagVO::getSlug).toList())
                .topicSlugs(topics.stream().map(BlogTopicVO::getSlug).toList())
                .build();
    }

    /*
     * 公开页面专用的专题标记：走 publicTopicsByPostId* 而不是 topicsByPostId*。
     * 停用专题不在前台展示，若仍留在卡片上，读者点进去只会拿到一个 404 页面。
     */
    /**
     * 公开专题列表所属处理入口。
     * 主要协作调用：<code>topicMapper.publicTopicsByPostId</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    private List<BlogTopicVO> publicTopicsOf(Long postId) {
        return topicMapper.publicTopicsByPostId(postId);
    }

    /**
     * 后台响应对象处理入口。
     * 关键处理步骤调用 <code>BlogPostAdminVO.builder</code>、<code>post.getId</code>、<code>post.getSlug</code>、<code>post.getTitle</code>。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @param tags 标签列表；类型为 <code>List&lt;BlogTagVO&gt;</code>
     * @param topics 专题列表；类型为 <code>List&lt;BlogTopicVO&gt;</code>
     * @return 类型为 <code>BlogPostAdminVO</code> 的处理结果
     */
    private BlogPostAdminVO adminVO(BlogPostEntity post, List<BlogTagVO> tags, List<BlogTopicVO> topics) {
        return BlogPostAdminVO.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .summary(summary(post))
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

    /**
     * 公开响应对象处理入口。
     * 关键处理步骤调用 <code>BlogPostPublicVO.builder</code>、<code>post.getId</code>、<code>post.getSlug</code>、<code>post.getTitle</code>。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @param tags 标签列表；类型为 <code>List&lt;BlogTagVO&gt;</code>
     * @param topics 专题列表；类型为 <code>List&lt;BlogTopicVO&gt;</code>
     * @return 类型为 <code>BlogPostPublicVO</code> 的处理结果
     */
    private BlogPostPublicVO publicVO(BlogPostEntity post, List<BlogTagVO> tags, List<BlogTopicVO> topics) {
        return BlogPostPublicVO.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .title(post.getTitle())
                .summary(summary(post))
                .coverMediaAssetId(post.getCoverMediaAssetId())
                .coverUrl(coverUrl(post.getCoverMediaAssetId()))
                .publishedAt(post.getPublishedAt())
                .updatedAt(post.getUpdatedAt())
                .tags(tags)
                .topics(topics)
                .build();
    }

    /**
     * 分组标签列表处理入口。
     * 关键处理步骤调用 <code>grouped.computeIfAbsent</code>、<code>row.getPostId</code>、<code>BlogTagVO.builder</code>、<code>row.getTagId</code>。
     *
     * @param rows 行集合；类型为 <code>List&lt;BlogPostTagRow&gt;</code>
     * @return 类型为 <code>Map&lt;Long, List&lt;BlogTagVO&gt;&gt;</code> 的处理结果
     */
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

    /**
     * 分组专题列表处理入口。
     * 关键处理步骤调用 <code>grouped.computeIfAbsent</code>、<code>row.getPostId</code>、<code>BlogTopicVO.builder</code>、<code>row.getTopicId</code>。
     *
     * @param rows 行集合；类型为 <code>List&lt;BlogPostTopicRow&gt;</code>
     * @return 类型为 <code>Map&lt;Long, List&lt;BlogTopicVO&gt;&gt;</code> 的处理结果
     */
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
