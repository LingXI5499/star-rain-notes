package com.starrainnotes.blog.service.impl;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.blog.constant.BlogLimits;
import com.starrainnotes.blog.constant.BlogMediaReference;
import com.starrainnotes.blog.dto.BlogPostBodyDTO;
import com.starrainnotes.blog.dto.BlogPostCreateDTO;
import com.starrainnotes.blog.dto.BlogPostQueryDTO;
import com.starrainnotes.blog.dto.BlogPostUpdateDTO;
import com.starrainnotes.blog.dto.BlogTopicMemberRow;
import com.starrainnotes.blog.dto.BlogTopicOrderItem;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.enumeration.BlogPostStatus;
import com.starrainnotes.blog.enumeration.BlogTaxonomyStatus;
import com.starrainnotes.blog.exception.BlogPostNotFoundException;
import com.starrainnotes.blog.exception.BlogPostSlugConflictException;
import com.starrainnotes.blog.exception.BlogPostSlugInvalidException;
import com.starrainnotes.blog.exception.BlogPostStateInvalidException;
import com.starrainnotes.blog.exception.BlogPostSummaryInvalidException;
import com.starrainnotes.blog.exception.BlogPostTitleInvalidException;
import com.starrainnotes.blog.exception.BlogTagDisabledException;
import com.starrainnotes.blog.exception.BlogTagNotFoundException;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.mapper.BlogTopicMapper;
import com.starrainnotes.blog.service.BlogPostService;
import com.starrainnotes.blog.service.BlogViewAssembler;
import com.starrainnotes.blog.utils.BlogContentMediaParser;
import com.starrainnotes.blog.utils.BlogMediaReferenceCommands;
import com.starrainnotes.blog.utils.BlogPostSlugDeriver;
import com.starrainnotes.blog.utils.BlogQueryRules;
import com.starrainnotes.blog.utils.BlogSlugRules;
import com.starrainnotes.blog.utils.BlogTextRules;
import com.starrainnotes.blog.vo.BlogPostAdminDetailVO;
import com.starrainnotes.blog.vo.BlogPostAdminVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.media.api.MediaReferenceApi;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * BLOG-003 创建 / 编辑 Post 的实现。
 *
 * 两条边界贯穿全类：
 * 1. 媒体引用必须在业务事务里登记与解除，否则会出现“文章有封面但引用表没有”，
 *    Media 的归档保护随之失效（媒体被归档，文章封面变成 404）。
 * 2. Tag 绑定只允许 ENABLED：停用标签的历史绑定保留，但不再接受新的绑定。
 */
@Service
public class BlogPostServiceImpl implements BlogPostService {

    private final BlogPostMapper postMapper;
    private final BlogTagMapper tagMapper;
    private final BlogTopicMapper topicMapper;
    private final MediaReferenceApi mediaReferenceApi;
    private final CurrentActorApi currentActorApi;
    private final BlogViewAssembler assembler;

    public BlogPostServiceImpl(BlogPostMapper postMapper,
                               BlogTagMapper tagMapper,
                               BlogTopicMapper topicMapper,
                               MediaReferenceApi mediaReferenceApi,
                               CurrentActorApi currentActorApi,
                               BlogViewAssembler assembler) {
        this.postMapper = postMapper;
        this.tagMapper = tagMapper;
        this.topicMapper = topicMapper;
        this.mediaReferenceApi = mediaReferenceApi;
        this.currentActorApi = currentActorApi;
        this.assembler = assembler;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BlogPostAdminVO> page(BlogPostQueryDTO query) {
        BlogQueryRules.validatePage(query.getPage(), query.getPageSize());
        String keyword = BlogTextRules.normalizeKeyword(query.getKeyword());
        BlogPostStatus status = BlogQueryRules.postStatus(query.getStatus());
        Long tagId = BlogQueryRules.optionalId(query.getTagId(), "tagId");
        Long topicId = BlogQueryRules.optionalId(query.getTopicId(), "topicId");

        long total = postMapper.adminPageCount(keyword, status == null ? null : status.name(), tagId, topicId);
        List<BlogPostAdminVO> items = total == 0
                ? List.of()
                : assembler.toAdminVOs(postMapper.adminPage(keyword, status == null ? null : status.name(),
                        tagId, topicId, BlogQueryRules.offset(query.getPage(), query.getPageSize()),
                        query.getPageSize()));
        return PageResult.<BlogPostAdminVO>builder()
                .items(items)
                .total(total)
                .page(query.getPage())
                .pageSize(query.getPageSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BlogPostAdminDetailVO detail(Long postId) {
        return assembler.toAdminDetailVO(requirePost(postId));
    }

    // 预览与详情同一份数据：差异只在前端呈现方式（预览按公开样式渲染）
    @Override
    @Transactional(readOnly = true)
    public BlogPostAdminDetailVO preview(Long postId) {
        return assembler.toAdminDetailVO(requirePost(postId));
    }

    @Override
    @Transactional
    public BlogPostAdminVO create(BlogPostCreateDTO request) {
        Long actorId = currentAccountId();
        String title = requireTitle(request.getTitle());
        String summary = normalizeSummary(request.getSummary());
        boolean derived = request.getSlug() == null || request.getSlug().isBlank();
        String baseSlug = derived ? BlogPostSlugDeriver.derive(title) : requireSlug(request.getSlug());

        BlogPostEntity entity = new BlogPostEntity();
        entity.setTitle(title);
        entity.setSummary(summary);
        // 正文允许暂时为空：草稿先建出来，正文走 PUT /body 单独提交
        entity.setBodyMarkdown("");
        entity.setCoverMediaAssetId(request.getCoverMediaAssetId());
        entity.setStatus(BlogPostStatus.DRAFT_CODE);
        entity.setCreatedByAccountId(actorId);
        entity.setUpdatedByAccountId(actorId);
        for (int ordinal = 1; ordinal <= 1000; ordinal++) {
            String candidate = derived ? BlogPostSlugDeriver.withSuffix(baseSlug, ordinal) : baseSlug;
            if (postMapper.countBySlug(candidate, null) > 0) {
                if (!derived) {
                    throw new BlogPostSlugConflictException();
                }
                continue;
            }
            entity.setSlug(candidate);
            try {
                postMapper.insertPost(entity);
                break;
            } catch (DuplicateKeyException exception) {
                // 并发创建可能在预检后抢占唯一键；自动 slug 换确定性后缀重试。
                if (!derived) {
                    throw new BlogPostSlugConflictException();
                }
            }
            if (ordinal == 1000) {
                throw new BlogPostSlugConflictException();
            }
        }
        if (entity.getId() == null) {
            throw new BlogPostSlugConflictException();
        }

        /*
         * 封面引用与文章在同一事务里登记。
         * 注意这里没有先写 cover_media_asset_id 再 attach：insertPost 已经带上它，
         * 但引用登记失败会整体回滚，不会留下“有封面无引用”的中间状态。
         */
        if (entity.getCoverMediaAssetId() != null) {
            mediaReferenceApi.attach(BlogMediaReferenceCommands.cover(entity.getCoverMediaAssetId(),
                    entity.getId()));
        }
        if (request.getTagIds() != null) {
            syncTags(entity.getId(), request.getTagIds());
        }
        return assembler.toAdminVO(requirePost(entity.getId()));
    }

    @Override
    @Transactional
    public BlogPostAdminVO update(Long postId, BlogPostUpdateDTO request) {
        Long actorId = currentAccountId();
        // 加行锁：封面引用的“读旧值 → 写新值 → 换引用”必须整体串行
        BlogPostEntity post = requirePostForUpdate(postId);

        String slug = post.getSlug();
        if (request.getSlug() != null) {
            slug = requireSlug(request.getSlug());
            if (!slug.equals(post.getSlug()) && postMapper.countBySlug(slug, postId) > 0) {
                throw new BlogPostSlugConflictException();
            }
        }
        String title = request.getTitle() == null ? post.getTitle() : requireTitle(request.getTitle());
        String summary = request.getSummary() == null ? post.getSummary() : normalizeSummary(request.getSummary());

        Long newCover = post.getCoverMediaAssetId();
        boolean coverChanged = false;
        if (Boolean.TRUE.equals(request.getClearCover())) {
            newCover = null;
            coverChanged = post.getCoverMediaAssetId() != null;
        } else if (request.getCoverMediaAssetId() != null
                && !request.getCoverMediaAssetId().equals(post.getCoverMediaAssetId())) {
            newCover = request.getCoverMediaAssetId();
            coverChanged = true;
        }

        postMapper.updatePostMeta(postId, slug, title, summary, actorId);

        if (coverChanged) {
            postMapper.updatePostCover(postId, newCover, actorId);
            /*
             * 顺序刻意是“先 attach 新封面，再 detach 旧封面”：
             * 反过来的话，若 attach 失败，事务回滚后旧封面字段仍是旧值，看似无恙；
             * 但若中途异常被吞掉，就会出现文章指向新封面却没有新引用。
             * 先建立新引用，任何失败都只可能留下“多一条引用”，比“引用缺失”安全得多。
             */
            if (newCover != null) {
                mediaReferenceApi.attach(BlogMediaReferenceCommands.cover(newCover, postId));
            }
            if (post.getCoverMediaAssetId() != null) {
                mediaReferenceApi.detach(BlogMediaReferenceCommands.cover(post.getCoverMediaAssetId(), postId));
            }
        }
        if (request.getTagIds() != null) {
            syncTags(postId, request.getTagIds());
        }
        return assembler.toAdminVO(requirePost(postId));
    }

    @Override
    @Transactional
    public BlogPostAdminDetailVO updateBody(Long postId, BlogPostBodyDTO request) {
        Long actorId = currentAccountId();
        BlogPostEntity post = requirePostForUpdate(postId);
        String body = request.getBodyMarkdown() == null ? "" : request.getBodyMarkdown();
        postMapper.updatePostBody(postId, body, actorId);

        /*
         * 正文里的媒体引用整体重建。
         *
         * 引用集合的真源就是正文本身（见 BlogContentMediaParser），因此这里
         * 先解除该文章的全部引用再按当前正文重建，而不是去算差集：
         * 算差集需要一份“当前引用列表”，而 Media 只按资产维度提供查询，
         * 硬要维护本地副本就又多了一份会漂移的数据。
         *
         * 副作用是封面引用也会被解掉再重建，所以下面必须把封面重新 attach 回去。
         */
        List<Long> contentIds = BlogContentMediaParser.extractMediaAssetIds(body);
        mediaReferenceApi.detachAll(BlogMediaReference.SOURCE_MODULE, BlogMediaReference.SOURCE_TYPE_POST, postId);
        if (post.getCoverMediaAssetId() != null) {
            mediaReferenceApi.attach(BlogMediaReferenceCommands.cover(post.getCoverMediaAssetId(), postId));
        }
        for (Long mediaAssetId : contentIds) {
            // attach 会校验媒体存在且 ACTIVE：正文里引用了已归档素材时立刻报错，而不是发布后才发现
            mediaReferenceApi.attach(BlogMediaReferenceCommands.content(mediaAssetId, postId));
        }
        return assembler.toAdminDetailVO(requirePost(postId));
    }

    @Override
    @Transactional
    public void delete(Long postId) {
        BlogPostEntity post = requirePostForUpdate(postId);
        if (BlogPostStatus.PUBLISHED_CODE.equals(post.getStatus())) {
            // 公开内容必须先撤回：直接删除会让已经传播出去的 URL 变成 404
            throw new BlogPostStateInvalidException("已发布文章必须先撤回再删除");
        }

        // 先记录它属于哪些专题，删完关系后要压缩这些专题的序号
        List<BlogTopicVO> topics = topicMapper.topicsByPostId(postId);

        // 关系先清理再删主记录：不留孤儿关系，也不依赖逻辑外键
        tagMapper.deletePostTagsByPostId(postId);
        topicMapper.deleteTopicPostsByPostId(postId);
        /*
         * 媒体引用必须一并解除。
         * 否则 Media 侧会永远认为这篇文章还在引用这些文件，归档会被永久拒绝，
         * 而这篇文章其实已经不存在了 —— 引用表变成只增不减的垃圾。
         */
        mediaReferenceApi.detachAll(BlogMediaReference.SOURCE_MODULE, BlogMediaReference.SOURCE_TYPE_POST, postId);

        int deleted = postMapper.deletePost(postId);
        if (deleted == 0) {
            // 并发场景：另一个请求刚刚发布了这篇文章，条件删除因此没有命中
            throw new BlogPostStateInvalidException("文章状态已变化，删除未生效");
        }
        for (BlogTopicVO topic : topics) {
            compactOrder(topic.getId());
        }
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private BlogPostEntity requirePost(Long postId) {
        BlogPostEntity post = postId == null ? null : postMapper.postById(postId);
        if (post == null) {
            throw new BlogPostNotFoundException();
        }
        return post;
    }

    private BlogPostEntity requirePostForUpdate(Long postId) {
        BlogPostEntity post = postId == null ? null : postMapper.postByIdForUpdate(postId);
        if (post == null) {
            throw new BlogPostNotFoundException();
        }
        return post;
    }

    private Long currentAccountId() {
        return currentActorApi.current().getAccountId();
    }

    private String requireSlug(String raw) {
        String slug = BlogSlugRules.normalize(raw);
        if (!BlogSlugRules.isValid(slug, BlogLimits.POST_SLUG_MAX_LENGTH)) {
            throw new BlogPostSlugInvalidException(
                    "slug 只能由小写字母、数字与中划线组成，且不超过 " + BlogLimits.POST_SLUG_MAX_LENGTH + " 个字符");
        }
        return slug;
    }

    private String requireTitle(String raw) {
        String title = BlogTextRules.normalizeName(raw);
        if (title == null) {
            throw new BlogPostTitleInvalidException("标题不能为空");
        }
        if (title.length() > BlogLimits.POST_TITLE_MAX_LENGTH) {
            throw new BlogPostTitleInvalidException("标题不能超过 " + BlogLimits.POST_TITLE_MAX_LENGTH + " 个字符");
        }
        return title;
    }

    // 空字符串表示“清空摘要”（发布时会按正文派生一份），null 由调用方解释为“不修改”
    private String normalizeSummary(String raw) {
        if (raw == null) {
            return null;
        }
        String summary = raw.trim();
        if (summary.isEmpty()) {
            return null;
        }
        if (summary.length() > BlogLimits.POST_SUMMARY_MAX_LENGTH) {
            throw new BlogPostSummaryInvalidException(
                    "摘要不能超过 " + BlogLimits.POST_SUMMARY_MAX_LENGTH + " 个字符");
        }
        return summary;
    }

    /*
     * 同步标签绑定。
     *
     * 先加后删：中途失败时宁可在事务回滚后回到原状，也不要出现“既没加也没删”的中间态。
     * 停用标签只拦新绑定：已经绑定的停用标签保留历史关系，否则停用一个标签会静默改写历史文章。
     */
    private void syncTags(Long postId, List<Long> tagIds) {
        Set<Long> target = new LinkedHashSet<>();
        for (Long tagId : tagIds) {
            if (tagId != null) {
                target.add(tagId);
            }
        }
        List<BlogTagVO> currentTags = tagMapper.tagsByPostId(postId);
        Set<Long> current = new LinkedHashSet<>();
        for (BlogTagVO tag : currentTags) {
            current.add(tag.getId());
        }

        for (Long tagId : target) {
            if (current.contains(tagId)) {
                continue;
            }
            // 加锁读取：避免与“停用该标签”的并发操作交错，出现停用后仍被绑定
            BlogTagEntity tag = tagMapper.tagByIdForUpdate(tagId);
            if (tag == null) {
                throw new BlogTagNotFoundException();
            }
            if (!BlogTaxonomyStatus.ENABLED_CODE.equals(tag.getStatus())) {
                throw new BlogTagDisabledException();
            }
            tagMapper.insertPostTag(postId, tagId);
        }
        for (Long tagId : current) {
            if (!target.contains(tagId)) {
                tagMapper.deletePostTag(postId, tagId);
            }
        }
    }

    // 删除文章后把受影响专题的序号重排回 1..n，保持“下一个序号 = 最大值 + 1”
    private void compactOrder(Long topicId) {
        List<BlogTopicMemberRow> members = topicMapper.topicMembers(topicId);
        if (members.isEmpty()) {
            return;
        }
        List<BlogTopicOrderItem> items = new ArrayList<>(members.size());
        for (int index = 0; index < members.size(); index++) {
            items.add(BlogTopicOrderItem.builder()
                    .postId(members.get(index).getPostId())
                    .sortOrder(index + 1)
                    .build());
        }
        topicMapper.updateTopicOrder(topicId, items);
    }
}
