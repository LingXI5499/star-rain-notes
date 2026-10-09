package com.starrainnotes.blog.service.impl;

import com.starrainnotes.blog.constant.BlogLimits;
import com.starrainnotes.blog.dto.BlogTagDTO;
import com.starrainnotes.blog.dto.BlogTagQueryDTO;
import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.enumeration.BlogTaxonomyStatus;
import com.starrainnotes.blog.exception.BlogTagDescriptionInvalidException;
import com.starrainnotes.blog.exception.BlogTagNameConflictException;
import com.starrainnotes.blog.exception.BlogTagNameInvalidException;
import com.starrainnotes.blog.exception.BlogTagNotFoundException;
import com.starrainnotes.blog.exception.BlogTagSlugConflictException;
import com.starrainnotes.blog.exception.BlogTagSlugInvalidException;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.service.BlogTagService;
import com.starrainnotes.blog.service.BlogViewAssembler;
import com.starrainnotes.blog.utils.BlogQueryRules;
import com.starrainnotes.blog.utils.BlogSlugDeriver;
import com.starrainnotes.blog.utils.BlogSlugRules;
import com.starrainnotes.blog.utils.BlogTextRules;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.common.result.PageResult;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * BLOG-004 标签管理的实现。
 *
 * Tag 的语义边界在这里被固化下来：
 * - 无序：没有任何排序字段与排序接口，需要顺序请用 Topic；
 * - 名字唯一：同名不同 ID 会把同一批文章拆到两个归档入口；
 * - 只能停用不能删除：已被文章使用的标签若被物理删除，历史归档会指向不存在的分类。
 */
@Service
public class BlogTagServiceImpl implements BlogTagService {

    private final BlogTagMapper tagMapper;
    private final BlogViewAssembler assembler;

    public BlogTagServiceImpl(BlogTagMapper tagMapper, BlogViewAssembler assembler) {
        this.tagMapper = tagMapper;
        this.assembler = assembler;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BlogTagVO> page(BlogTagQueryDTO query) {
        BlogQueryRules.validatePage(query.getPage(), query.getPageSize());
        String keyword = BlogTextRules.normalizeKeyword(query.getKeyword());
        BlogTaxonomyStatus status = BlogQueryRules.taxonomyStatus(query.getStatus());

        long total = tagMapper.adminPageCount(keyword, status == null ? null : status.name());
        List<BlogTagVO> items = total == 0
                ? List.of()
                : tagMapper.adminPage(keyword, status == null ? null : status.name(),
                        BlogQueryRules.offset(query.getPage(), query.getPageSize()), query.getPageSize());
        return PageResult.<BlogTagVO>builder()
                .items(items)
                .total(total)
                .page(query.getPage())
                .pageSize(query.getPageSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogTagVO> listPublished() {
        return tagMapper.publishedTags();
    }

    @Override
    @Transactional
    public BlogTagVO create(BlogTagDTO request) {
        String name = requireName(request.getName());
        String description = requireDescription(request.getDescription());
        boolean automatic = request.getSlug() == null || request.getSlug().isBlank();
        String base = automatic
                ? BlogSlugDeriver.derive(name, "tag", BlogLimits.TAG_SLUG_MAX_LENGTH)
                : requireSlug(request.getSlug());
        if (!automatic) {
            assertSlugFree(base, null);
        }
        assertNameFree(name, null);

        for (int ordinal = 1; ordinal <= (automatic ? 1000 : 1); ordinal++) {
            String slug = BlogSlugDeriver.withSuffix(base, ordinal, BlogLimits.TAG_SLUG_MAX_LENGTH);
            if (automatic && tagMapper.countBySlug(slug, null) > 0) {
                continue;
            }
            BlogTagEntity entity = new BlogTagEntity();
            entity.setSlug(slug);
            entity.setName(name);
            entity.setDescription(description);
            entity.setStatus(BlogTaxonomyStatus.ENABLED_CODE);
            try {
                tagMapper.insertTag(entity);
                return assembler.toTagVO(requireTag(entity.getId()));
            } catch (DuplicateKeyException ex) {
                // 并发创建同名标签仍报名称冲突；仅自动生成的 slug 才换后缀重试。
                if (!automatic) {
                    throw duplicateReason(slug, name, null);
                }
                assertNameFree(name, null);
            }
        }
        throw new BlogTagSlugConflictException();
    }

    @Override
    @Transactional
    public BlogTagVO update(Long tagId, BlogTagDTO request) {
        BlogTagEntity tag = requireTagForUpdate(tagId);
        String slug = request.getSlug() == null ? tag.getSlug() : requireSlug(request.getSlug());
        String name = request.getName() == null ? tag.getName() : requireName(request.getName());
        // description 允许被显式清空：传空字符串就是清空说明
        String description = request.getDescription() == null
                ? tag.getDescription()
                : requireDescription(request.getDescription());

        if (!slug.equals(tag.getSlug())) {
            assertSlugFree(slug, tagId);
        }
        if (!name.equals(tag.getName())) {
            assertNameFree(name, tagId);
        }
        try {
            tagMapper.updateTag(tagId, slug, name, description);
        } catch (DuplicateKeyException ex) {
            throw duplicateReason(slug, name, tagId);
        }
        return assembler.toTagVO(requireTag(tagId));
    }

    /*
     * 停用：不再允许新绑定，但不动任何已有绑定关系。
     * 重复停用视为成功（幂等）：管理员连点两次不该看到一个业务错误。
     */
    @Override
    @Transactional
    public void disable(Long tagId) {
        BlogTagEntity tag = requireTagForUpdate(tagId);
        if (BlogTaxonomyStatus.DISABLED_CODE.equals(tag.getStatus())) {
            return;
        }
        tagMapper.updateStatus(tagId, BlogTaxonomyStatus.DISABLED_CODE);
    }

    @Override
    @Transactional
    public void enable(Long tagId) {
        BlogTagEntity tag = requireTagForUpdate(tagId);
        if (BlogTaxonomyStatus.ENABLED_CODE.equals(tag.getStatus())) {
            return;
        }
        tagMapper.updateStatus(tagId, BlogTaxonomyStatus.ENABLED_CODE);
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private BlogTagEntity requireTag(Long tagId) {
        BlogTagEntity tag = tagId == null ? null : tagMapper.tagById(tagId);
        if (tag == null) {
            throw new BlogTagNotFoundException();
        }
        return tag;
    }

    private BlogTagEntity requireTagForUpdate(Long tagId) {
        BlogTagEntity tag = tagId == null ? null : tagMapper.tagByIdForUpdate(tagId);
        if (tag == null) {
            throw new BlogTagNotFoundException();
        }
        return tag;
    }

    private void assertSlugFree(String slug, Long excludeId) {
        if (tagMapper.countBySlug(slug, excludeId) > 0) {
            throw new BlogTagSlugConflictException();
        }
    }

    private void assertNameFree(String name, Long excludeId) {
        if (tagMapper.countByName(name, excludeId) > 0) {
            throw new BlogTagNameConflictException();
        }
    }

    private RuntimeException duplicateReason(String slug, String name, Long excludeId) {
        if (tagMapper.countBySlug(slug, excludeId) > 0) {
            return new BlogTagSlugConflictException();
        }
        return new BlogTagNameConflictException();
    }

    private String requireSlug(String raw) {
        String slug = BlogSlugRules.normalize(raw);
        if (!BlogSlugRules.isValid(slug, BlogLimits.TAG_SLUG_MAX_LENGTH)) {
            throw new BlogTagSlugInvalidException(
                    "slug 只能由小写字母、数字与中划线组成，且不超过 " + BlogLimits.TAG_SLUG_MAX_LENGTH + " 个字符");
        }
        return slug;
    }

    private String requireName(String raw) {
        String name = BlogTextRules.normalizeName(raw);
        if (name == null) {
            throw new BlogTagNameInvalidException("标签名不能为空");
        }
        if (name.length() > BlogLimits.TAG_NAME_MAX_LENGTH) {
            throw new BlogTagNameInvalidException("标签名不能超过 " + BlogLimits.TAG_NAME_MAX_LENGTH + " 个字符");
        }
        return name;
    }

    private String requireDescription(String raw) {
        String description = BlogTextRules.normalizeDescription(raw);
        if (description != null && description.length() > BlogLimits.TAG_DESCRIPTION_MAX_LENGTH) {
            throw new BlogTagDescriptionInvalidException(
                    "标签说明不能超过 " + BlogLimits.TAG_DESCRIPTION_MAX_LENGTH + " 个字符");
        }
        return description;
    }
}
