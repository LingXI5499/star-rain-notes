/*
 * BlogTagServiceImpl：博客模块的业务实现文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：BlogLimits、BlogTagDTO、BlogTagQueryDTO、BlogTagEntity、BlogTaxonomyStatus、BlogTagDescriptionInvalidException、BlogTagNameConflictException。
 * 阅读入口：page（分页读取博客标签）；listPublished（列出已发布）；create（创建博客标签）；update（更新博客标签）。
 */

package com.starrainnotes.blog.service.impl;

import com.starrainnotes.blog.constant.BlogLimits;
import com.starrainnotes.blog.dto.BlogTagDTO;
import com.starrainnotes.blog.dto.BlogTagQueryDTO;
import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.enumeration.BlogTaxonomyStatus;
import com.starrainnotes.blog.exception.BlogTagDescriptionInvalidException;
import com.starrainnotes.blog.exception.BlogTagInUseException;
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
 * - 未使用标签可删除；已被文章使用的标签保留绑定，使用停用或先显式解除关联。
 */
/**
 * 博客标签业务服务实现，属于博客模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class BlogTagServiceImpl implements BlogTagService {
    /** Match binding writers' row lock, so checking and deleting cannot race a new binding. */
    @Override
    @Transactional
    public void delete(Long tagId) {
        requireTagForUpdate(tagId);
        if (tagMapper.countReferences(tagId) > 0 || tagMapper.deleteUnusedTag(tagId) != 1) {
            throw new BlogTagInUseException();
        }
    }

    /**
     * 标签数据映射。
     * 协作类型：<code>BlogTagMapper</code>。
     */
    private final BlogTagMapper tagMapper;
    /**
     * 组装器。
     */
    private final BlogViewAssembler assembler;

    /**
     * 构造BlogTagServiceImpl，接收本类使用的依赖或初始化参数。
     *
     * @param tagMapper 标签数据映射；类型为 <code>BlogTagMapper</code>
     * @param assembler 组装器；类型为 <code>BlogViewAssembler</code>
     */
    public BlogTagServiceImpl(BlogTagMapper tagMapper, BlogViewAssembler assembler) {
        this.tagMapper = tagMapper;
        this.assembler = assembler;
    }

    /**
     * 分页读取博客标签。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tagMapper.adminPageCount</code>、<code>tagMapper.adminPage</code>。
     *
     * @param query 查询参数；类型为 <code>BlogTagQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;BlogTagVO&gt;</code> 的处理结果
     */
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

    /**
     * 列出已发布。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tagMapper.publishedTags</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    @Transactional(readOnly = true)
    public List<BlogTagVO> listPublished() {
        return tagMapper.publishedTags();
    }

    /**
     * 创建博客标签。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tagMapper.countBySlug</code>、<code>tagMapper.insertTag</code>、<code>assembler.toTagVO</code>。
     * 实现中的显式失败类型包括 <code>BlogTagSlugConflictException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>BlogTagDTO</code>
     * @return 类型为 <code>BlogTagVO</code> 的处理结果
     */
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

    /**
     * 更新博客标签。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tagMapper.updateTag</code>、<code>assembler.toTagVO</code>。
     *
     * @param tagId 标签标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>BlogTagDTO</code>
     * @return 类型为 <code>BlogTagVO</code> 的处理结果
     */
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
    /**
     * 禁用博客标签。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tagMapper.updateStatus</code>。
     *
     * @param tagId 标签标识；类型为 <code>Long</code>
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

    /**
     * 启用博客标签。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tagMapper.updateStatus</code>。
     *
     * @param tagId 标签标识；类型为 <code>Long</code>
     */
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

    /**
     * 检查所需标签。
     * 主要协作调用：<code>tagMapper.tagById</code>。
     * 实现中的显式失败类型包括 <code>BlogTagNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param tagId 标签标识；类型为 <code>Long</code>
     * @return 类型为 <code>BlogTagEntity</code> 的处理结果
     */
    private BlogTagEntity requireTag(Long tagId) {
        BlogTagEntity tag = tagId == null ? null : tagMapper.tagById(tagId);
        if (tag == null) {
            throw new BlogTagNotFoundException();
        }
        return tag;
    }

    /**
     * 检查所需标签面向更新。
     * 主要协作调用：<code>tagMapper.tagByIdForUpdate</code>。
     * 实现中的显式失败类型包括 <code>BlogTagNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param tagId 标签标识；类型为 <code>Long</code>
     * @return 类型为 <code>BlogTagEntity</code> 的处理结果
     */
    private BlogTagEntity requireTagForUpdate(Long tagId) {
        BlogTagEntity tag = tagId == null ? null : tagMapper.tagByIdForUpdate(tagId);
        if (tag == null) {
            throw new BlogTagNotFoundException();
        }
        return tag;
    }

    /**
     * 校验固定链接标识Free。
     * 主要协作调用：<code>tagMapper.countBySlug</code>。
     * 实现中的显式失败类型包括 <code>BlogTagSlugConflictException</code>；具体触发条件见对应分支。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param excludeId exclude标识；类型为 <code>Long</code>
     */
    private void assertSlugFree(String slug, Long excludeId) {
        if (tagMapper.countBySlug(slug, excludeId) > 0) {
            throw new BlogTagSlugConflictException();
        }
    }

    /**
     * 校验名称Free。
     * 主要协作调用：<code>tagMapper.countByName</code>。
     * 实现中的显式失败类型包括 <code>BlogTagNameConflictException</code>；具体触发条件见对应分支。
     *
     * @param name 名称；类型为 <code>String</code>
     * @param excludeId exclude标识；类型为 <code>Long</code>
     */
    private void assertNameFree(String name, Long excludeId) {
        if (tagMapper.countByName(name, excludeId) > 0) {
            throw new BlogTagNameConflictException();
        }
    }

    /**
     * duplicate原因处理入口。
     * 主要协作调用：<code>tagMapper.countBySlug</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param name 名称；类型为 <code>String</code>
     * @param excludeId exclude标识；类型为 <code>Long</code>
     * @return 类型为 <code>RuntimeException</code> 的处理结果
     */
    private RuntimeException duplicateReason(String slug, String name, Long excludeId) {
        if (tagMapper.countBySlug(slug, excludeId) > 0) {
            return new BlogTagSlugConflictException();
        }
        return new BlogTagNameConflictException();
    }

    /**
     * 检查所需固定链接标识。
     * 关键处理步骤调用 <code>BlogSlugRules.normalize</code>、<code>BlogSlugRules.isValid</code>。
     * 实现中的显式失败类型包括 <code>BlogTagSlugInvalidException</code>；具体触发条件见对应分支。
     *
     * @param raw 原始；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String requireSlug(String raw) {
        String slug = BlogSlugRules.normalize(raw);
        if (!BlogSlugRules.isValid(slug, BlogLimits.TAG_SLUG_MAX_LENGTH)) {
            throw new BlogTagSlugInvalidException(
                    "slug 只能由小写字母、数字与中划线组成，且不超过 " + BlogLimits.TAG_SLUG_MAX_LENGTH + " 个字符");
        }
        return slug;
    }

    /**
     * 检查所需名称。
     * 关键处理步骤调用 <code>BlogTextRules.normalizeName</code>、<code>name.length</code>。
     * 实现中的显式失败类型包括 <code>BlogTagNameInvalidException</code>；具体触发条件见对应分支。
     *
     * @param raw 原始；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
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

    /**
     * 检查所需描述。
     * 关键处理步骤调用 <code>BlogTextRules.normalizeDescription</code>、<code>description.length</code>。
     * 实现中的显式失败类型包括 <code>BlogTagDescriptionInvalidException</code>；具体触发条件见对应分支。
     *
     * @param raw 原始；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String requireDescription(String raw) {
        String description = BlogTextRules.normalizeDescription(raw);
        if (description != null && description.length() > BlogLimits.TAG_DESCRIPTION_MAX_LENGTH) {
            throw new BlogTagDescriptionInvalidException(
                    "标签说明不能超过 " + BlogLimits.TAG_DESCRIPTION_MAX_LENGTH + " 个字符");
        }
        return description;
    }
}
