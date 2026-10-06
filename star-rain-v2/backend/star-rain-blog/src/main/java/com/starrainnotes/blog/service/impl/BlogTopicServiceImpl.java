package com.starrainnotes.blog.service.impl;

import com.starrainnotes.blog.constant.BlogLimits;
import com.starrainnotes.blog.dto.BlogTopicDTO;
import com.starrainnotes.blog.dto.BlogTopicMemberRow;
import com.starrainnotes.blog.dto.BlogTopicOrderItem;
import com.starrainnotes.blog.dto.BlogTopicQueryDTO;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.entity.BlogTopicEntity;
import com.starrainnotes.blog.enumeration.BlogTaxonomyStatus;
import com.starrainnotes.blog.exception.BlogPostNotFoundException;
import com.starrainnotes.blog.exception.BlogQueryInvalidException;
import com.starrainnotes.blog.exception.BlogTopicDescriptionInvalidException;
import com.starrainnotes.blog.exception.BlogTopicDisabledException;
import com.starrainnotes.blog.exception.BlogTopicNameInvalidException;
import com.starrainnotes.blog.exception.BlogTopicNotEmptyException;
import com.starrainnotes.blog.exception.BlogTopicNotFoundException;
import com.starrainnotes.blog.exception.BlogTopicPostExistsException;
import com.starrainnotes.blog.exception.BlogTopicPostNotFoundException;
import com.starrainnotes.blog.exception.BlogTopicSlugConflictException;
import com.starrainnotes.blog.exception.BlogTopicSlugInvalidException;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.mapper.BlogTopicMapper;
import com.starrainnotes.blog.service.BlogTopicService;
import com.starrainnotes.blog.utils.BlogQueryRules;
import com.starrainnotes.blog.utils.BlogSlugDeriver;
import com.starrainnotes.blog.utils.BlogSlugRules;
import com.starrainnotes.blog.utils.BlogTextRules;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.result.PageResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * BLOG-005 / BLOG-006 / BLOG-007 的实现。
 *
 * Topic 与 Tag 的差别在这里最明显：
 * - 名字允许重复（只保证 slug 唯一），因为专题是“给人看的一组文章”；
 * - 成员有 sortOrder，加入追加到末尾，移出压缩序号，重排要求整集合一致；
 * - 停用只影响“能否加入新文章”与前台入口，成员与顺序全部保留。
 *
 * 序号不变量：某专题的成员序号恒为 1..n 连续。
 * 靠两件事维持 —— 加入时取 max + 1，移出后按当前顺序重写 1..n。
 * 这样“顺序”可以用列表下标表达，前端拖拽结果能直接映射成接口出入参。
 */
@Service
public class BlogTopicServiceImpl implements BlogTopicService {

    private final BlogTopicMapper topicMapper;
    private final BlogPostMapper postMapper;

    public BlogTopicServiceImpl(BlogTopicMapper topicMapper, BlogPostMapper postMapper) {
        this.topicMapper = topicMapper;
        this.postMapper = postMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BlogTopicVO> page(BlogTopicQueryDTO query) {
        BlogQueryRules.validatePage(query.getPage(), query.getPageSize());
        String keyword = BlogTextRules.normalizeKeyword(query.getKeyword());
        BlogTaxonomyStatus status = BlogQueryRules.taxonomyStatus(query.getStatus());

        long total = topicMapper.adminPageCount(keyword, status == null ? null : status.name());
        List<BlogTopicVO> items = total == 0
                ? List.of()
                : topicMapper.adminPage(keyword, status == null ? null : status.name(),
                        BlogQueryRules.offset(query.getPage(), query.getPageSize()), query.getPageSize());
        return PageResult.<BlogTopicVO>builder()
                .items(items)
                .total(total)
                .page(query.getPage())
                .pageSize(query.getPageSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogTopicVO> listPublished() {
        return topicMapper.publishedTopics();
    }

    @Override
    @Transactional
    public BlogTopicVO create(BlogTopicDTO request) {
        String name = requireName(request.getName());
        String description = requireDescription(request.getDescription());
        boolean automatic = request.getSlug() == null || request.getSlug().isBlank();
        String base = automatic
                ? BlogSlugDeriver.derive(name, "topic", BlogLimits.TOPIC_SLUG_MAX_LENGTH)
                : requireSlug(request.getSlug());

        for (int ordinal = 1; ordinal <= (automatic ? 1000 : 1); ordinal++) {
            String slug = BlogSlugDeriver.withSuffix(base, ordinal, BlogLimits.TOPIC_SLUG_MAX_LENGTH);
            if (topicMapper.countBySlug(slug, null) > 0) {
                if (automatic) {
                    continue;
                }
                throw new BlogTopicSlugConflictException();
            }
            BlogTopicEntity entity = new BlogTopicEntity();
            entity.setSlug(slug);
            entity.setName(name);
            entity.setDescription(description);
            Integer minimum = topicMapper.minimumSortOrder();
            entity.setSortOrder(minimum == null ? 0 : minimum - 10);
            entity.setFeatured(request.getFeatured() == null || request.getFeatured());
            entity.setStatus(BlogTaxonomyStatus.ENABLED_CODE);
            try {
                topicMapper.insertTopic(entity);
                return toTopicVO(requireTopic(entity.getId()));
            } catch (DuplicateKeyException ex) {
                if (!automatic) {
                    throw new BlogTopicSlugConflictException();
                }
            }
        }
        throw new BlogTopicSlugConflictException();
    }

    @Override
    @Transactional
    public BlogTopicVO update(Long topicId, BlogTopicDTO request) {
        BlogTopicEntity topic = requireTopicForUpdate(topicId);
        String slug = request.getSlug() == null ? topic.getSlug() : requireSlug(request.getSlug());
        String name = request.getName() == null ? topic.getName() : requireName(request.getName());
        String description = request.getDescription() == null
                ? topic.getDescription()
                : requireDescription(request.getDescription());

        if (!slug.equals(topic.getSlug()) && topicMapper.countBySlug(slug, topicId) > 0) {
            throw new BlogTopicSlugConflictException();
        }
        try {
            topicMapper.updateTopic(topicId, slug, name, description);
            if (request.getFeatured() != null) {
                topicMapper.updatePresentation(topicId, topic.getSortOrder(), request.getFeatured());
            }
        } catch (DuplicateKeyException ex) {
            throw new BlogTopicSlugConflictException();
        }
        return toTopicVO(requireTopic(topicId));
    }

    @Override
    @Transactional
    public void reorderTopics(List<Long> topicIds) {
        List<BlogTopicVO> current = topicMapper.adminPage(null, null, 0, Integer.MAX_VALUE);
        Set<Long> expected = new LinkedHashSet<>();
        current.forEach(topic -> expected.add(topic.getId()));
        /*
         * 这里的校验不含 `topicIds.contains(null)`：List.of(...) 这类不可变列表
         * 碰到 contains(null) 会直接抛 NPE（ImmutableCollections 显式拒绝 null 查询），
         * 于是「顺序非法」在单测里表现为 500 而不是 400。
         * 去掉它不改变判定结果：带 null 的列表要么长度对不上，要么集合与 expected 不相等，
         * 下面这一行仍然会拒绝。
         */
        if (topicIds == null || topicIds.size() != expected.size()
                || !new LinkedHashSet<>(topicIds).equals(expected)) {
            throw new BlogQueryInvalidException("专题排序必须包含全部专题且不能重复");
        }
        for (int index = 0; index < topicIds.size(); index++) {
            Long id = topicIds.get(index);
            BlogTopicEntity topic = requireTopicForUpdate(id);
            topicMapper.updatePresentation(id, (index + 1) * 10, Boolean.TRUE.equals(topic.getFeatured()));
        }
    }

    /*
     * 删除专题：只对空专题开放。
     *
     * 专题的价值主要在成员与顺序上，物理删除会连带毁掉策展结果，
     * 所以这里先数成员而不是级联删除关系：有成员就报 BLOG_TOPIC_NOT_EMPTY，
     * 由调用方显式走 removePost（那条路径会压缩序号，顺序仍然是 1..n）。
     *
     * 取行锁再数：与「同时给这个专题加文章」并发时，
     * 否则可能删掉一个刚被加入成员、关系已经写进去的专题。
     */
    @Override
    @Transactional
    public void delete(Long topicId) {
        BlogTopicEntity topic = requireTopicForUpdate(topicId);
        if (topicMapper.countTopicPosts(topic.getId()) > 0) {
            throw new BlogTopicNotEmptyException();
        }
        topicMapper.deleteTopic(topic.getId());
    }

    /*
     * 停用：不在前台展示、不再接受新文章，但成员关系与顺序全部保留。
     * 幂等：重复停用不报错。
     */
    @Override
    @Transactional
    public void disable(Long topicId) {
        BlogTopicEntity topic = requireTopicForUpdate(topicId);
        if (BlogTaxonomyStatus.DISABLED_CODE.equals(topic.getStatus())) {
            return;
        }
        topicMapper.updateStatus(topicId, BlogTaxonomyStatus.DISABLED_CODE);
    }

    @Override
    @Transactional
    public void enable(Long topicId) {
        BlogTopicEntity topic = requireTopicForUpdate(topicId);
        if (BlogTaxonomyStatus.ENABLED_CODE.equals(topic.getStatus())) {
            return;
        }
        topicMapper.updateStatus(topicId, BlogTaxonomyStatus.ENABLED_CODE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogTopicMemberRow> members(Long topicId) {
        requireTopic(topicId);
        return topicMapper.topicMembers(topicId);
    }

    // BLOG-006 加入：追加到末尾，序号 = 当前最大值 + 1
    @Override
    @Transactional
    public void addPost(Long topicId, Long postId) {
        BlogTopicEntity topic = requireTopicForUpdate(topicId);
        if (!BlogTaxonomyStatus.ENABLED_CODE.equals(topic.getStatus())) {
            throw new BlogTopicDisabledException();
        }
        BlogPostEntity post = postId == null ? null : postMapper.postById(postId);
        if (post == null) {
            throw new BlogPostNotFoundException();
        }
        if (topicMapper.countTopicPost(topicId, postId) > 0) {
            throw new BlogTopicPostExistsException();
        }
        Integer max = topicMapper.maxSortOrder(topicId);
        int nextOrder = max == null ? 1 : max + 1;
        try {
            topicMapper.insertTopicPost(topicId, postId, nextOrder);
        } catch (DuplicateKeyException ex) {
            throw new BlogTopicPostExistsException();
        }
    }

    /*
     * BLOG-006 移出：先删关系，再按剩余成员重写 1..n。
     * 停用的专题也允许移出 —— 只能加不能减会让“专题不再合适”的文章无法清理。
     */
    @Override
    @Transactional
    public void removePost(Long topicId, Long postId) {
        requireTopicForUpdate(topicId);
        if (topicMapper.countTopicPost(topicId, postId) == 0) {
            throw new BlogTopicPostNotFoundException();
        }
        topicMapper.deleteTopicPost(topicId, postId);
        compactOrder(topicId);
    }

    /*
     * BLOG-007 重排。
     *
     * 要求 postIds 与该专题当前成员集合完全一致，而不是“允许只传子集”：
     * 后者会让未出现在请求里的成员序号留在旧值上，形成重复序号，
     * 排序结果就取决于数据库的物理顺序，同一个专题每次读到的顺序可能不同。
     */
    @Override
    @Transactional
    public void reorder(Long topicId, List<Long> postIds) {
        requireTopicForUpdate(topicId);
        List<BlogTopicMemberRow> members = topicMapper.topicMembers(topicId);
        List<Long> requested = postIds == null ? List.of() : postIds;

        if (members.isEmpty() && requested.isEmpty()) {
            // 空专题重排空列表：无操作，不算错误
            return;
        }
        if (requested.isEmpty()) {
            throw new BlogQueryInvalidException("postIds 不能为空");
        }

        Set<Long> unique = new LinkedHashSet<>();
        for (Long postId : requested) {
            if (postId == null) {
                throw new BlogQueryInvalidException("postIds 不能包含空值");
            }
            if (!unique.add(postId)) {
                throw new BlogQueryInvalidException("postIds 不能包含重复的文章 ID");
            }
        }
        Set<Long> current = new LinkedHashSet<>();
        for (BlogTopicMemberRow member : members) {
            current.add(member.getPostId());
        }
        for (Long postId : unique) {
            if (!current.contains(postId)) {
                throw new BlogTopicPostNotFoundException();
            }
        }
        if (unique.size() != current.size()) {
            throw new BlogQueryInvalidException("postIds 必须包含该专题的全部文章");
        }

        List<BlogTopicOrderItem> items = new ArrayList<>(requested.size());
        for (int index = 0; index < requested.size(); index++) {
            items.add(BlogTopicOrderItem.builder()
                    .postId(requested.get(index))
                    .sortOrder(index + 1)
                    .build());
        }
        topicMapper.updateTopicOrder(topicId, items);
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    // 按当前顺序重写 1..n，维持“序号连续”的不变量
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

    private BlogTopicVO toTopicVO(BlogTopicEntity topic) {
        return BlogTopicVO.builder()
                .id(topic.getId())
                .slug(topic.getSlug())
                .name(topic.getName())
                .description(topic.getDescription())
                .sortOrder(topic.getSortOrder())
                .featured(topic.getFeatured())
                .status(topic.getStatus())
                .memberCount(topicMapper.countTopicPosts(topic.getId()))
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                .build();
    }

    private BlogTopicEntity requireTopic(Long topicId) {
        BlogTopicEntity topic = topicId == null ? null : topicMapper.topicById(topicId);
        if (topic == null) {
            throw new BlogTopicNotFoundException();
        }
        return topic;
    }

    private BlogTopicEntity requireTopicForUpdate(Long topicId) {
        BlogTopicEntity topic = topicId == null ? null : topicMapper.topicByIdForUpdate(topicId);
        if (topic == null) {
            throw new BlogTopicNotFoundException();
        }
        return topic;
    }

    private String requireSlug(String raw) {
        String slug = BlogSlugRules.normalize(raw);
        if (!BlogSlugRules.isValid(slug, BlogLimits.TOPIC_SLUG_MAX_LENGTH)) {
            throw new BlogTopicSlugInvalidException(
                    "slug 只能由小写字母、数字与中划线组成，且不超过 " + BlogLimits.TOPIC_SLUG_MAX_LENGTH + " 个字符");
        }
        return slug;
    }

    private String requireName(String raw) {
        String name = BlogTextRules.normalizeName(raw);
        if (name == null) {
            throw new BlogTopicNameInvalidException("专题名不能为空");
        }
        if (name.length() > BlogLimits.TOPIC_NAME_MAX_LENGTH) {
            throw new BlogTopicNameInvalidException("专题名不能超过 " + BlogLimits.TOPIC_NAME_MAX_LENGTH + " 个字符");
        }
        return name;
    }

    private String requireDescription(String raw) {
        String description = BlogTextRules.normalizeDescription(raw);
        if (description != null && description.length() > BlogLimits.TOPIC_DESCRIPTION_MAX_LENGTH) {
            throw new BlogTopicDescriptionInvalidException(
                    "专题说明不能超过 " + BlogLimits.TOPIC_DESCRIPTION_MAX_LENGTH + " 个字符");
        }
        return description;
    }
}
