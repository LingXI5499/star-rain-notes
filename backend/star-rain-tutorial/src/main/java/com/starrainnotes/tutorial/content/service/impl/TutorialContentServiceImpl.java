/*
 * TutorialContentServiceImpl：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：TutorialRevisionMapper、CurrentActorApi、PageResult、ChapterBodyDTO、ChapterCreateDTO、ChapterUpdateDTO、TutorialCreateDTO。
 * 阅读入口：categories（categories处理入口）；createCategory（创建分类）；updateCategory（更新分类）；deleteCategory（删除分类）。
 */

package com.starrainnotes.tutorial.content.service.impl;

import org.springframework.transaction.annotation.Isolation;
import com.starrainnotes.tutorial.content.mapper.TutorialRevisionMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.dto.ChapterBodyDTO;
import com.starrainnotes.tutorial.content.dto.ChapterCreateDTO;
import com.starrainnotes.tutorial.content.dto.ChapterUpdateDTO;
import com.starrainnotes.tutorial.content.dto.TutorialCreateDTO;
import com.starrainnotes.tutorial.content.dto.TutorialUpdateDTO;
import com.starrainnotes.tutorial.content.entity.TutorialCategoryEntity;
import com.starrainnotes.tutorial.content.entity.TutorialChapterEntity;
import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import com.starrainnotes.tutorial.content.entity.TutorialGroupEntity;
import com.starrainnotes.tutorial.content.exception.TutorialCategoryNotFoundException;
import com.starrainnotes.tutorial.content.exception.TutorialChapterNotFoundException;
import com.starrainnotes.tutorial.content.exception.TutorialConflictException;
import com.starrainnotes.tutorial.content.exception.TutorialGroupNotEmptyException;
import com.starrainnotes.tutorial.content.exception.TutorialGroupNotFoundException;
import com.starrainnotes.tutorial.content.exception.TutorialInvalidRequestException;
import com.starrainnotes.tutorial.content.exception.TutorialNotFoundException;
import com.starrainnotes.tutorial.content.exception.TutorialStateException;
import com.starrainnotes.tutorial.content.mapper.TutorialCategoryMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialChapterMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialDependencyCleanup;
import com.starrainnotes.tutorial.content.mapper.TutorialGroupMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.service.TutorialContentService;
import com.starrainnotes.tutorial.content.media.TutorialMediaReferences;
import com.starrainnotes.tutorial.content.utils.TutorialSlugDeriver;
import com.starrainnotes.tutorial.content.vo.TutorialAdminVO;
import com.starrainnotes.tutorial.content.vo.TutorialCategoryVO;
import com.starrainnotes.tutorial.content.vo.TutorialChapterVO;
import com.starrainnotes.tutorial.content.vo.TutorialCurriculumVO;
import com.starrainnotes.tutorial.content.vo.TutorialGroupVO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教程内容业务服务实现，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class TutorialContentServiceImpl implements TutorialContentService {
    /**
     * 分类数据映射。
     * 协作类型：<code>TutorialCategoryMapper</code>。
     */
    private final TutorialCategoryMapper categoryMapper;
    /**
     * 教程数据映射。
     * 协作类型：<code>TutorialMapper</code>。
     */
    private final TutorialMapper tutorialMapper;
    /**
     * 分组数据映射。
     * 协作类型：<code>TutorialGroupMapper</code>。
     */
    private final TutorialGroupMapper groupMapper;
    /**
     * 章节数据映射。
     * 协作类型：<code>TutorialChapterMapper</code>。
     */
    private final TutorialChapterMapper chapterMapper;
    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi currentActorApi;
    /**
     * 媒体References。
     */
    private final TutorialMediaReferences mediaReferences;
    /**
     * dependencies。
     */
    private final TutorialDependencyCleanup dependencies;
    /**
     * 修订数据映射。
     * 协作类型：<code>TutorialRevisionMapper</code>。
     */
    private final TutorialRevisionMapper revisionMapper;

    /**
     * now处理入口。
     * 关键处理步骤调用 <code>LocalDateTime.now</code>。
     * @return 类型为 <code>LocalDateTime</code> 的处理结果
     */
    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * 文本处理入口。
     * 关键处理步骤调用 <code>value.strip</code>、<code>result.isEmpty</code>、<code>result.length</code>。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @param field 字段；类型为 <code>String</code>
     * @param maxLength 最大Length；类型为 <code>int</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private static String text(String value, String field, int maxLength) {
        String result = value == null ? "" : value.strip();
        if (result.isEmpty() || result.length() > maxLength) {
            throw new TutorialInvalidRequestException(field + "不能为空，且长度不能超过 " + maxLength + " 字符");
        }
        return result;
    }

    /**
     * optional文本处理入口。
     * 关键处理步骤调用 <code>value.strip</code>、<code>result.length</code>、<code>result.isEmpty</code>。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @param maxLength 最大Length；类型为 <code>int</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private static String optionalText(String value, int maxLength) {
        String result = value == null ? "" : value.strip();
        if (result.length() > maxLength) {
            throw new TutorialInvalidRequestException("内容长度不能超过 " + maxLength + " 字符");
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * 正文文本处理入口。
     * 关键处理步骤调用 <code>value.isBlank</code>、<code>value.length</code>。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private static String bodyText(String value) {
        if (value == null || value.isBlank() || value.length() > 2_000_000) {
            throw new TutorialInvalidRequestException("章节正文不能为空，且长度不能超过 2000000 字符");
        }
        return value;
    }

    /**
     * 检查所需分类。
     * 主要协作调用：<code>categoryMapper.selectById</code>。
     * 实现中的显式失败类型包括 <code>TutorialCategoryNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialCategoryEntity</code> 的处理结果
     */
    private TutorialCategoryEntity requireCategory(Long id) {
        TutorialCategoryEntity category = id == null ? null : categoryMapper.selectById(id);
        if (category == null) throw new TutorialCategoryNotFoundException();
        return category;
    }

    /**
     * 检查所需教程。
     * 主要协作调用：<code>tutorialMapper.selectById</code>。
     * 实现中的显式失败类型包括 <code>TutorialNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialEntity</code> 的处理结果
     */
    private TutorialEntity requireTutorial(Long id) {
        TutorialEntity tutorial = id == null ? null : tutorialMapper.selectById(id);
        if (tutorial == null) throw new TutorialNotFoundException();
        return tutorial;
    }

    /**
     * 锁教程处理入口。
     * 主要协作调用：<code>tutorialMapper.byIdForUpdate</code>。
     * 实现中的显式失败类型包括 <code>TutorialNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialEntity</code> 的处理结果
     */
    private TutorialEntity lockTutorial(Long id) {
        TutorialEntity tutorial = id == null ? null : tutorialMapper.byIdForUpdate(id);
        if (tutorial == null) throw new TutorialNotFoundException();
        return tutorial;
    }

    /**
     * editable教程处理入口。
     * 关键处理步骤调用 <code>lockTutorial</code>、<code>tutorial.getEditingStatus</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialEntity</code> 的处理结果
     */
    private TutorialEntity editableTutorial(Long id) {
        TutorialEntity tutorial = lockTutorial(id);
        if ("IN_REVIEW".equals(tutorial.getEditingStatus())) {
            throw new TutorialStateException("教程审核中，请先完成或取消审核再修改工作区");
        }
        return tutorial;
    }

    /**
     * 检查所需分组。
     * 主要协作调用：<code>groupMapper.selectById</code>。
     * 实现中的显式失败类型包括 <code>TutorialGroupNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialGroupEntity</code> 的处理结果
     */
    private TutorialGroupEntity requireGroup(Long id) {
        TutorialGroupEntity group = id == null ? null : groupMapper.selectById(id);
        if (group == null) throw new TutorialGroupNotFoundException();
        return group;
    }

    /**
     * 检查所需章节。
     * 主要协作调用：<code>chapterMapper.selectById</code>。
     * 实现中的显式失败类型包括 <code>TutorialChapterNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialChapterEntity</code> 的处理结果
     */
    private TutorialChapterEntity requireChapter(Long id) {
        TutorialChapterEntity chapter = id == null ? null : chapterMapper.selectById(id);
        if (chapter == null) throw new TutorialChapterNotFoundException();
        return chapter;
    }

    /**
     * 分类行集合处理入口。
     * 主要协作调用：<code>categoryMapper.listOrdered</code>。
     * @return 本次处理得到的条目集合
     */
    private List<TutorialCategoryEntity> categoryRows() {
        return categoryMapper.listOrdered();
    }

    /**
     * 教程行集合处理入口。
     * 主要协作调用：<code>tutorialMapper.listOrdered</code>。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    private List<TutorialEntity> tutorialRows(Long categoryId) {
        return tutorialMapper.listOrdered(categoryId);
    }

    /**
     * 分组行集合处理入口。
     * 主要协作调用：<code>groupMapper.listByTutorialId</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    private List<TutorialGroupEntity> groupRows(Long tutorialId) {
        return groupMapper.listByTutorialId(tutorialId);
    }

    /**
     * 章节行集合处理入口。
     * 主要协作调用：<code>chapterMapper.listByGroupId</code>。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    private List<TutorialChapterEntity> chapterRows(Long groupId) {
        return chapterMapper.listByGroupId(groupId);
    }

    /**
     * 下一项顺序处理入口。
     * 关键处理步骤调用 <code>orders.stream</code>。
     *
     * @param orders orders；类型为 <code>List&lt;Integer&gt;</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    private static int nextOrder(List<Integer> orders) {
        return orders.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).max().orElse(0) + 10;
    }

    /**
     * 检查所需Exact顺序。
     * 关键处理步骤调用 <code>ids.size</code>、<code>expected.size</code>。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @param expected 测试期望结果；类型为 <code>List&lt;Long&gt;</code>
     */
    private static void requireExactOrder(List<Long> ids, List<Long> expected) {
        if (ids == null || ids.size() != expected.size()
                || new HashSet<>(ids).size() != ids.size()
                || !new HashSet<>(ids).equals(new HashSet<>(expected))) {
            throw new TutorialInvalidRequestException("排序必须包含当前层的全部对象，且不能重复");
        }
    }

    /**
     * 分类View处理入口。
     * 关键处理步骤调用 <code>TutorialCategoryVO.builder</code>、<code>String.valueOf</code>、<code>category.getId</code>、<code>category.getName</code>。
     *
     * @param category 分类；类型为 <code>TutorialCategoryEntity</code>
     * @return 类型为 <code>TutorialCategoryVO</code> 的处理结果
     */
    private TutorialCategoryVO categoryView(TutorialCategoryEntity category) {
        return TutorialCategoryVO.builder().id(String.valueOf(category.getId())).name(category.getName())
                .slug(category.getSlug()).sortOrder(category.getSortOrder())
                .tutorialCount(category.getTutorialCount() == null ? 0L : category.getTutorialCount())
                .chapterCount(category.getChapterCount() == null ? 0L : category.getChapterCount()).build();
    }

    /**
     * 教程View处理入口。
     * 主要协作调用：<code>categoryMapper.selectById</code>、<code>chapterMapper.countByTutorialId</code>。
     *
     * @param tutorial 教程；类型为 <code>TutorialEntity</code>
     * @return 类型为 <code>TutorialAdminVO</code> 的处理结果
     */
    private TutorialAdminVO tutorialView(TutorialEntity tutorial) {
        TutorialCategoryEntity category = categoryMapper.selectById(tutorial.getCategoryId());
        long chapterCount = chapterMapper.countByTutorialId(tutorial.getId());
        return TutorialAdminVO.builder().id(String.valueOf(tutorial.getId()))
                .categoryId(String.valueOf(tutorial.getCategoryId()))
                .categoryName(category == null ? null : category.getName())
                .slug(tutorial.getSlug()).title(tutorial.getTitle()).summary(tutorial.getSummary())
                .sortOrder(tutorial.getSortOrder()).publicationStatus(tutorial.getPublicationStatus())
                .editingStatus(tutorial.getEditingStatus()).chapterCount(chapterCount)
                .publishedAt(tutorial.getPublishedAt()).updatedAt(tutorial.getUpdatedAt()).build();
    }

    /**
     * 分组View处理入口。
     * 关键处理步骤调用 <code>TutorialGroupVO.builder</code>、<code>String.valueOf</code>、<code>group.getId</code>、<code>group.getTutorialId</code>。
     *
     * @param group 分组；类型为 <code>TutorialGroupEntity</code>
     * @param chapters 章节列表；类型为 <code>List&lt;TutorialChapterEntity&gt;</code>
     * @return 类型为 <code>TutorialGroupVO</code> 的处理结果
     */
    private TutorialGroupVO groupView(TutorialGroupEntity group, List<TutorialChapterEntity> chapters) {
        return TutorialGroupVO.builder().id(String.valueOf(group.getId()))
                .tutorialId(String.valueOf(group.getTutorialId())).title(group.getTitle())
                .description(group.getDescription()).sortOrder(group.getSortOrder())
                .status(group.getStatus()).chapterCount(chapters.size())
                .chapters(chapters.stream().map(chapter -> {
                    TutorialChapterVO item = chapterView(chapter);
                    item.setBodyMarkdown(null);
                    return item;
                }).toList()).build();
    }

    /**
     * 章节View处理入口。
     * 关键处理步骤调用 <code>TutorialChapterVO.builder</code>、<code>String.valueOf</code>、<code>chapter.getId</code>、<code>chapter.getTutorialId</code>。
     *
     * @param chapter 章节；类型为 <code>TutorialChapterEntity</code>
     * @return 类型为 <code>TutorialChapterVO</code> 的处理结果
     */
    private TutorialChapterVO chapterView(TutorialChapterEntity chapter) {
        return TutorialChapterVO.builder().id(String.valueOf(chapter.getId()))
                .tutorialId(String.valueOf(chapter.getTutorialId()))
                .groupId(String.valueOf(chapter.getGroupId()))
                .slug(chapter.getSlug()).title(chapter.getTitle()).summary(chapter.getSummary())
                .bodyMarkdown(chapter.getBodyMarkdown()).sortOrder(chapter.getSortOrder())
                .status(chapter.getStatus()).rowVersion(chapter.getRowVersion()).updatedAt(chapter.getUpdatedAt()).build();
    }

    /**
     * categories处理入口。
     * 关键处理步骤调用 <code>categoryRows</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<TutorialCategoryVO> categories() {
        return categoryRows().stream().map(this::categoryView).toList();
    }

    /**
     * 创建分类。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>categoryMapper.countBySlug</code>、<code>categoryMapper.insert</code>。
     * 实现中的显式失败类型包括 <code>TutorialConflictException</code>；具体触发条件见对应分支。
     *
     * @param name 名称；类型为 <code>String</code>
     * @return 类型为 <code>TutorialCategoryVO</code> 的处理结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TutorialCategoryVO createCategory(String name) {
        String validName = text(name, "知识体系名称", 100);
        String base = TutorialSlugDeriver.fromTitle(validName, "category");
        TutorialCategoryEntity category = new TutorialCategoryEntity();
        category.setName(validName);
        category.setSortOrder(nextOrder(categoryRows().stream().map(TutorialCategoryEntity::getSortOrder).toList()));
        for (int attempt = 1; attempt <= 8; attempt++) {
            String slug = attempt == 1 ? base : base + "-" + attempt;
            if (categoryMapper.countBySlug(slug) > 0) continue;
            category.setSlug(slug);
            try {
                categoryMapper.insert(category);
                return categoryView(category);
            } catch (DuplicateKeyException ignored) {
                // 同名并发创建时唯一索引是最终裁决，下一次尝试使用可读后缀。
            }
        }
        throw new TutorialConflictException("知识体系名称对应的地址已被占用，请稍后重试");
    }

    /**
     * 更新分类。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>categoryMapper.updateName</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param name 名称；类型为 <code>String</code>
     * @return 类型为 <code>TutorialCategoryVO</code> 的处理结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TutorialCategoryVO updateCategory(Long id, String name) {
        TutorialCategoryEntity category = requireCategory(id);
        category.setName(text(name, "知识体系名称", 100));
        category.setUpdatedAt(now());
        // 公开地址保持稳定：改名不会破坏已有链接。
        categoryMapper.updateName(category.getId(), category.getName(), category.getUpdatedAt());
        return categoryView(category);
    }

    /**
     * 删除分类。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tutorialMapper.countByCategoryId</code>、<code>categoryMapper.deleteById</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteCategory(Long id) {
        requireCategory(id);
        if (tutorialMapper.countByCategoryId(id) > 0) {
            throw new TutorialStateException("知识体系仍有教程，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    /**
     * 调整顺序Categories。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>categoryMapper.updateSortOrder</code>。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void reorderCategories(List<Long> ids) {
        List<TutorialCategoryEntity> rows = categoryRows();
        requireExactOrder(ids, rows.stream().map(TutorialCategoryEntity::getId).toList());
        for (int index = 0; index < ids.size(); index++) {
            final Long id = ids.get(index);
            TutorialCategoryEntity row = rows.stream().filter(item -> item.getId().equals(id))
                    .findFirst().orElseThrow();
            row.setSortOrder((index + 1) * 10);
            row.setUpdatedAt(now());
            categoryMapper.updateSortOrder(row.getId(), row.getSortOrder(), row.getUpdatedAt());
        }
    }

    /**
     * tutorials处理入口。
     * 关键处理步骤调用 <code>tutorials</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;TutorialAdminVO&gt;</code> 的处理结果
     */
    @Override
    public PageResult<TutorialAdminVO> tutorials(int page, int pageSize) {
        return tutorials(null, null, page, pageSize);
    }

    /**
     * tutorials处理入口。
     * 主要协作调用：<code>tutorialMapper.adminPage</code>、<code>tutorialMapper.adminCount</code>。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @param keyword 关键词；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;TutorialAdminVO&gt;</code> 的处理结果
     */
    @Override
    public PageResult<TutorialAdminVO> tutorials(Long categoryId, String keyword, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new TutorialInvalidRequestException("分页参数不合法");
        }
        if (categoryId != null && categoryId <= 0) throw new TutorialInvalidRequestException("分类参数不合法");
        String term = optionalText(keyword, 100);
        long start = (long) (page - 1) * pageSize;
        List<TutorialAdminVO> items = tutorialMapper.adminPage(categoryId, term, start, pageSize)
                .stream().map(this::tutorialView).toList();
        return PageResult.<TutorialAdminVO>builder().items(items).total(tutorialMapper.adminCount(categoryId, term))
                .page(page).pageSize(pageSize).build();
    }

    /**
     * 教程顺序处理入口。
     * 主要协作调用：<code>tutorialMapper.orderItems</code>。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @return 类型为 <code>java.util.List&lt;com.starrainnotes.tutorial.content.vo.TutorialOrderItemVO&gt;</code> 的处理结果
     */
    @Override
    public java.util.List<com.starrainnotes.tutorial.content.vo.TutorialOrderItemVO> tutorialOrder(Long categoryId) {
        requireCategory(categoryId);
        return tutorialMapper.orderItems(categoryId);
    }

    /**
     * 教程处理入口。
     * 关键处理步骤调用 <code>tutorialView</code>、<code>requireTutorial</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialAdminVO</code> 的处理结果
     */
    @Override
    public TutorialAdminVO tutorial(Long id) {
        return tutorialView(requireTutorial(id));
    }

    /**
     * 创建教程。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>tutorialMapper.countBySlug</code>、<code>tutorialMapper.insert</code>。
     * 实现中的显式失败类型包括 <code>TutorialConflictException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>TutorialCreateDTO</code>
     * @return 类型为 <code>TutorialAdminVO</code> 的处理结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TutorialAdminVO createTutorial(TutorialCreateDTO request) {
        requireCategory(request.getCategoryId());
        String title = text(request.getTitle(), "教程标题", 200);
        String summary = text(request.getSummary(), "教程摘要", 1000);
        String base = TutorialSlugDeriver.fromTitle(title, "tutorial");
        TutorialEntity tutorial = new TutorialEntity();
        tutorial.setCategoryId(request.getCategoryId());
        tutorial.setTitle(title);
        tutorial.setSummary(summary);
        tutorial.setSortOrder(nextOrder(tutorialRows(request.getCategoryId()).stream()
                .map(TutorialEntity::getSortOrder).toList()));
        tutorial.setPublicationStatus("NEVER_PUBLISHED");
        tutorial.setEditingStatus("DRAFT");
        tutorial.setCreatedByAccountId(currentActorApi.current().getAccountId());
        tutorial.setUpdatedByAccountId(tutorial.getCreatedByAccountId());
        for (int attempt = 1; attempt <= 8; attempt++) {
            String slug = attempt == 1 ? base : base + "-" + attempt;
            if (tutorialMapper.countBySlug(slug) > 0) continue;
            tutorial.setSlug(slug);
            try {
                tutorialMapper.insert(tutorial);
                return tutorialView(tutorial);
            } catch (DuplicateKeyException ignored) {
                // 并发同名创建使用下一个后缀，不把唯一键冲突变成 500。
            }
        }
        throw new TutorialConflictException("教程地址生成冲突，请稍后重试");
    }

    /**
     * 更新教程。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>tutorialMapper.update</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>TutorialUpdateDTO</code>
     * @return 类型为 <code>TutorialAdminVO</code> 的处理结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TutorialAdminVO updateTutorial(Long id, TutorialUpdateDTO request) {
        TutorialEntity tutorial = editableTutorial(id);
        if (request.getCategoryId() != null && !request.getCategoryId().equals(tutorial.getCategoryId())) {
            requireCategory(request.getCategoryId());
            tutorial.setCategoryId(request.getCategoryId());
            tutorial.setSortOrder(nextOrder(tutorialRows(request.getCategoryId()).stream()
                    .map(TutorialEntity::getSortOrder).toList()));
        }
        if (request.getTitle() != null) tutorial.setTitle(text(request.getTitle(), "教程标题", 200));
        if (request.getSummary() != null) tutorial.setSummary(text(request.getSummary(), "教程摘要", 1000));
        tutorial.setUpdatedByAccountId(currentActorApi.current().getAccountId());
        tutorial.setUpdatedAt(now());
        tutorialMapper.update(tutorial);
        return tutorialView(tutorial);
    }

    /**
     * 删除教程。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>revisionMapper.countByTutorialId</code>、<code>dependencies.tutorialLearning</code>、<code>groupMapper.deleteById</code>、<code>dependencies.revisionIds</code>、<code>mediaReferences.detachRevision</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteTutorial(Long id) {
        TutorialEntity tutorial = editableTutorial(id);
        if ("PUBLISHED".equals(tutorial.getPublicationStatus()))
            throw new TutorialStateException("请先撤回教程，再删除");
        if (revisionMapper.countByTutorialId(id) > 0) throw new TutorialStateException("教程存在历史版本，请使用撤回以保留学习记录");
        dependencies.tutorialLearning(id);
        for (TutorialGroupEntity group : groupRows(id)) {
            for (TutorialChapterEntity chapter : chapterRows(group.getId())) deleteChapterData(chapter);
            groupMapper.deleteById(group.getId());
        }
        for (Long revisionId : dependencies.revisionIds(id)) mediaReferences.detachRevision(revisionId);
        dependencies.revisions(id);
        tutorialMapper.deleteById(id);
    }

    /**
     * 删除章节Data。
     * 主要协作调用：<code>revisionMapper.countByTutorialId</code>、<code>dependencies.chapterChildren</code>、<code>mediaReferences.detachChapter</code>、<code>chapterMapper.deleteById</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param chapter 章节；类型为 <code>TutorialChapterEntity</code>
     */
    private void deleteChapterData(TutorialChapterEntity chapter) {
        if (revisionMapper.countByTutorialId(chapter.getTutorialId()) > 0) {
            throw new TutorialStateException("教程存在历史版本，请撤回章节以保留学习记录");
        }
        dependencies.chapterChildren(chapter.getId());
        mediaReferences.detachChapter(chapter.getId());
        chapterMapper.deleteById(chapter.getId());
    }

    /**
     * 调整顺序Tutorials。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tutorialMapper.updateSortOrder</code>。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void reorderTutorials(Long categoryId, List<Long> ids) {
        requireCategory(categoryId);
        List<TutorialEntity> rows = tutorialRows(categoryId);
        requireExactOrder(ids, rows.stream().map(TutorialEntity::getId).toList());
        for (int index = 0; index < ids.size(); index++) {
            final Long id = ids.get(index);
            TutorialEntity row = rows.stream().filter(item -> item.getId().equals(id)).findFirst().orElseThrow();
            row.setSortOrder((index + 1) * 10);
            row.setUpdatedAt(now());
            editableTutorial(row.getId());
            tutorialMapper.updateSortOrder(row.getId(), row.getSortOrder());
        }
    }

    /**
     * curriculum处理入口。
     * 关键处理步骤调用 <code>requireTutorial</code>、<code>groupRows</code>、<code>groupView</code>、<code>chapterRows</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialCurriculumVO</code> 的处理结果
     */
    @Override
    public TutorialCurriculumVO curriculum(Long tutorialId) {
        TutorialEntity tutorial = requireTutorial(tutorialId);
        List<TutorialGroupVO> groups = groupRows(tutorialId).stream()
                .map(group -> groupView(group, chapterRows(group.getId()))).toList();
        return TutorialCurriculumVO.builder().tutorial(tutorialView(tutorial)).groups(groups).build();
    }

    /**
     * 创建分组。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>groupMapper.insert</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @param title 标题；类型为 <code>String</code>
     * @return 类型为 <code>TutorialGroupVO</code> 的处理结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TutorialGroupVO createGroup(Long tutorialId, String title) {
        editableTutorial(tutorialId);
        TutorialGroupEntity group = new TutorialGroupEntity();
        group.setTutorialId(tutorialId);
        group.setTitle(text(title, "分组名称", 200));
        group.setSortOrder(nextOrder(groupRows(tutorialId).stream()
                .map(TutorialGroupEntity::getSortOrder).toList()));
        group.setStatus("ACTIVE");
        groupMapper.insert(group);
        return groupView(group, List.of());
    }

    /**
     * 更新分组。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>groupMapper.updateTitle</code>。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     * @param title 标题；类型为 <code>String</code>
     * @return 类型为 <code>TutorialGroupVO</code> 的处理结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TutorialGroupVO updateGroup(Long groupId, String title) {
        TutorialGroupEntity group = requireGroup(groupId);
        editableTutorial(group.getTutorialId());
        group.setTitle(text(title, "分组名称", 200));
        group.setUpdatedAt(now());
        groupMapper.updateTitle(group.getId(), group.getTitle(), group.getUpdatedAt());
        return groupView(group, chapterRows(groupId));
    }

    /**
     * 删除分组。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>dependencies.groupLearning</code>、<code>groupMapper.deleteById</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteGroup(Long groupId) {
        TutorialGroupEntity group = requireGroup(groupId);
        TutorialEntity tutorial = editableTutorial(group.getTutorialId());
        if ("PUBLISHED".equals(tutorial.getPublicationStatus()))
            throw new TutorialStateException("请先撤回教程，再删除分组");
        for (TutorialChapterEntity chapter : chapterRows(groupId)) deleteChapterData(chapter);
        dependencies.groupLearning(groupId);
        groupMapper.deleteById(groupId);
    }

    /**
     * 调整顺序Groups。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>groupMapper.updateSortOrder</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void reorderGroups(Long tutorialId, List<Long> ids) {
        editableTutorial(tutorialId);
        List<TutorialGroupEntity> rows = groupRows(tutorialId);
        requireExactOrder(ids, rows.stream().map(TutorialGroupEntity::getId).toList());
        for (int index = 0; index < ids.size(); index++) {
            final Long id = ids.get(index);
            TutorialGroupEntity row = rows.stream().filter(item -> item.getId().equals(id))
                    .findFirst().orElseThrow();
            row.setSortOrder((index + 1) * 10);
            row.setUpdatedAt(now());
            groupMapper.updateSortOrder(row.getId(), row.getSortOrder(), row.getUpdatedAt());
        }
    }

    /**
     * 章节处理入口。
     * 关键处理步骤调用 <code>chapterView</code>、<code>requireChapter</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialChapterVO</code> 的处理结果
     */
    @Override
    public TutorialChapterVO chapter(Long chapterId) {
        return chapterView(requireChapter(chapterId));
    }

    /**
     * 创建章节。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>chapterMapper.countByTutorialIdAndSlug</code>、<code>chapterMapper.insert</code>、<code>mediaReferences.replaceChapter</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>、<code>TutorialConflictException</code>；具体触发条件见对应分支。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterCreateDTO</code>
     * @return 类型为 <code>TutorialChapterVO</code> 的处理结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TutorialChapterVO createChapter(Long groupId, ChapterCreateDTO request) {
        TutorialGroupEntity group = requireGroup(groupId);
        if (!"ACTIVE".equals(group.getStatus())) throw new TutorialStateException("不能在已归档分组中新建章节");
        editableTutorial(group.getTutorialId());
        String title = text(request.getTitle(), "章节标题", 200);
        String body = bodyText(request.getBodyMarkdown());
        String base = TutorialSlugDeriver.fromTitle(title, "chapter");
        TutorialChapterEntity chapter = new TutorialChapterEntity();
        chapter.setTutorialId(group.getTutorialId());
        chapter.setGroupId(groupId);
        chapter.setTitle(title);
        chapter.setSummary(optionalText(request.getSummary(), 1000));
        chapter.setBodyMarkdown(body);
        chapter.setSortOrder(nextOrder(chapterRows(groupId).stream()
                .map(TutorialChapterEntity::getSortOrder).toList()));
        chapter.setStatus("DRAFT");
        for (int attempt = 1; attempt <= 8; attempt++) {
            String slug = attempt == 1 ? base : base + "-" + attempt;
            if (chapterMapper.countByTutorialIdAndSlug(group.getTutorialId(), slug) > 0) continue;
            chapter.setSlug(slug);
            try {
                chapterMapper.insert(chapter);
            } catch (DuplicateKeyException ignored) {
                // 同一教程内章节地址唯一，并发创建时按顺序尝试后缀。
                continue;
            }
            mediaReferences.replaceChapter(chapter.getId(), body);
            return chapterView(chapter);
        }
        throw new TutorialConflictException("章节地址生成冲突，请稍后重试");
    }

    /**
     * 更新章节。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>chapterMapper.saveWorkspace</code>、<code>mediaReferences.replaceChapter</code>。
     * 实现中的显式失败类型包括 <code>TutorialConflictException</code>；具体触发条件见对应分支。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterUpdateDTO</code>
     * @return 类型为 <code>TutorialChapterVO</code> 的处理结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TutorialChapterVO updateChapter(Long chapterId, ChapterUpdateDTO request) {
        TutorialChapterEntity chapter = requireChapter(chapterId);
        editableTutorial(chapter.getTutorialId());
        if (request.getRowVersion() == null || !request.getRowVersion().equals(chapter.getRowVersion())) {
            throw new TutorialConflictException("章节已被其他操作修改，请保留当前输入并重新加载后合并");
        }
        if (request.getTitle() != null) chapter.setTitle(text(request.getTitle(), "章节标题", 200));
        chapter.setSummary(optionalText(request.getSummary(), 1000));
        if (request.getBodyMarkdown() != null) chapter.setBodyMarkdown(bodyText(request.getBodyMarkdown()));
        chapter.setUpdatedAt(now());
        if (chapterMapper.saveWorkspace(chapter) != 1) {
            throw new TutorialConflictException("章节版本冲突，请重新加载后合并");
        }
        mediaReferences.replaceChapter(chapterId, chapter.getBodyMarkdown());
        chapter.setRowVersion(chapter.getRowVersion() + 1);
        return chapterView(chapter);
    }

    /**
     * 更新章节正文。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>requireChapter</code>、<code>updateChapter</code>、<code>ChapterUpdateDTO.builder</code>、<code>chapter.getTitle</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterBodyDTO</code>
     * @return 类型为 <code>TutorialChapterVO</code> 的处理结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TutorialChapterVO updateChapterBody(Long chapterId, ChapterBodyDTO request) {
        TutorialChapterEntity chapter = requireChapter(chapterId);
        return updateChapter(chapterId, ChapterUpdateDTO.builder().title(chapter.getTitle())
                .summary(chapter.getSummary()).bodyMarkdown(request.getBodyMarkdown())
                .rowVersion(request.getRowVersion()).build());
    }

    /**
     * 删除章节。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>requireChapter</code>、<code>editableTutorial</code>、<code>chapter.getTutorialId</code>、<code>tutorial.getPublicationStatus</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteChapter(Long chapterId) {
        TutorialChapterEntity chapter = requireChapter(chapterId);
        TutorialEntity tutorial = editableTutorial(chapter.getTutorialId());
        if ("PUBLISHED".equals(tutorial.getPublicationStatus()))
            throw new TutorialStateException("请先撤回教程，再删除章节");
        deleteChapterData(chapter);
    }

    /**
     * 调整顺序章节列表。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>chapterMapper.updateSortOrder</code>。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void reorderChapters(Long groupId, List<Long> ids) {
        TutorialGroupEntity group = requireGroup(groupId);
        editableTutorial(group.getTutorialId());
        List<TutorialChapterEntity> rows = chapterRows(groupId);
        requireExactOrder(ids, rows.stream().map(TutorialChapterEntity::getId).toList());
        for (int index = 0; index < ids.size(); index++) {
            final Long id = ids.get(index);
            TutorialChapterEntity row = rows.stream().filter(item -> item.getId().equals(id))
                    .findFirst().orElseThrow();
            row.setSortOrder((index + 1) * 10);
            row.setUpdatedAt(now());
            chapterMapper.updateSortOrder(row.getId(), row.getSortOrder(), row.getUpdatedAt());
        }
    }
}
