package com.starrainnotes.tutorial.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

@Service
@RequiredArgsConstructor
public class TutorialContentServiceImpl implements TutorialContentService {
    private final TutorialCategoryMapper categoryMapper;
    private final TutorialMapper tutorialMapper;
    private final TutorialGroupMapper groupMapper;
    private final TutorialChapterMapper chapterMapper;
    private final CurrentActorApi currentActorApi;
    private final TutorialMediaReferences mediaReferences;

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private static String text(String value, String field, int maxLength) {
        String result = value == null ? "" : value.strip();
        if (result.isEmpty() || result.length() > maxLength) {
            throw new TutorialInvalidRequestException(field + "不能为空，且长度不能超过 " + maxLength + " 字符");
        }
        return result;
    }

    private static String optionalText(String value, int maxLength) {
        String result = value == null ? "" : value.strip();
        if (result.length() > maxLength) {
            throw new TutorialInvalidRequestException("内容长度不能超过 " + maxLength + " 字符");
        }
        return result.isEmpty() ? null : result;
    }

    private static String bodyText(String value) {
        if (value == null || value.isBlank() || value.length() > 2_000_000) {
            throw new TutorialInvalidRequestException("章节正文不能为空，且长度不能超过 2000000 字符");
        }
        return value;
    }

    private TutorialCategoryEntity requireCategory(Long id) {
        TutorialCategoryEntity category = id == null ? null : categoryMapper.selectById(id);
        if (category == null) throw new TutorialCategoryNotFoundException();
        return category;
    }

    private TutorialEntity requireTutorial(Long id) {
        TutorialEntity tutorial = id == null ? null : tutorialMapper.selectById(id);
        if (tutorial == null) throw new TutorialNotFoundException();
        return tutorial;
    }

    private TutorialEntity lockTutorial(Long id) {
        TutorialEntity tutorial = id == null ? null : tutorialMapper.byIdForUpdate(id);
        if (tutorial == null) throw new TutorialNotFoundException();
        return tutorial;
    }

    private TutorialGroupEntity requireGroup(Long id) {
        TutorialGroupEntity group = id == null ? null : groupMapper.selectById(id);
        if (group == null) throw new TutorialGroupNotFoundException();
        return group;
    }

    private TutorialChapterEntity requireChapter(Long id) {
        TutorialChapterEntity chapter = id == null ? null : chapterMapper.selectById(id);
        if (chapter == null) throw new TutorialChapterNotFoundException();
        return chapter;
    }

    private List<TutorialCategoryEntity> categoryRows() {
        return categoryMapper.selectList(new LambdaQueryWrapper<TutorialCategoryEntity>()
                .orderByAsc(TutorialCategoryEntity::getSortOrder, TutorialCategoryEntity::getId));
    }

    private List<TutorialEntity> tutorialRows(Long categoryId) {
        LambdaQueryWrapper<TutorialEntity> query = new LambdaQueryWrapper<TutorialEntity>()
                .orderByAsc(TutorialEntity::getSortOrder, TutorialEntity::getId);
        if (categoryId != null) query.eq(TutorialEntity::getCategoryId, categoryId);
        return tutorialMapper.selectList(query);
    }

    private List<TutorialGroupEntity> groupRows(Long tutorialId) {
        return groupMapper.selectList(new LambdaQueryWrapper<TutorialGroupEntity>()
                .eq(TutorialGroupEntity::getTutorialId, tutorialId)
                .orderByAsc(TutorialGroupEntity::getSortOrder, TutorialGroupEntity::getId));
    }

    private List<TutorialChapterEntity> chapterRows(Long groupId) {
        return chapterMapper.selectList(new LambdaQueryWrapper<TutorialChapterEntity>()
                .eq(TutorialChapterEntity::getGroupId, groupId)
                .orderByAsc(TutorialChapterEntity::getSortOrder, TutorialChapterEntity::getId));
    }

    private static int nextOrder(List<Integer> orders) {
        return orders.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).max().orElse(0) + 10;
    }

    private static void requireExactOrder(List<Long> ids, List<Long> expected) {
        if (ids == null || ids.size() != expected.size()
                || new HashSet<>(ids).size() != ids.size()
                || !new HashSet<>(ids).equals(new HashSet<>(expected))) {
            throw new TutorialInvalidRequestException("排序必须包含当前层的全部对象，且不能重复");
        }
    }

    private TutorialCategoryVO categoryView(TutorialCategoryEntity category) {
        return TutorialCategoryVO.builder().id(String.valueOf(category.getId())).name(category.getName())
                .slug(category.getSlug()).sortOrder(category.getSortOrder()).build();
    }

    private TutorialAdminVO tutorialView(TutorialEntity tutorial) {
        TutorialCategoryEntity category = categoryMapper.selectById(tutorial.getCategoryId());
        long chapterCount = chapterMapper.selectCount(new LambdaQueryWrapper<TutorialChapterEntity>()
                .eq(TutorialChapterEntity::getTutorialId, tutorial.getId()));
        return TutorialAdminVO.builder().id(String.valueOf(tutorial.getId()))
                .categoryId(String.valueOf(tutorial.getCategoryId()))
                .categoryName(category == null ? null : category.getName())
                .slug(tutorial.getSlug()).title(tutorial.getTitle()).summary(tutorial.getSummary())
                .sortOrder(tutorial.getSortOrder()).publicationStatus(tutorial.getPublicationStatus())
                .editingStatus(tutorial.getEditingStatus()).chapterCount(chapterCount)
                .publishedAt(tutorial.getPublishedAt()).updatedAt(tutorial.getUpdatedAt()).build();
    }

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

    private TutorialChapterVO chapterView(TutorialChapterEntity chapter) {
        return TutorialChapterVO.builder().id(String.valueOf(chapter.getId()))
                .tutorialId(String.valueOf(chapter.getTutorialId()))
                .groupId(String.valueOf(chapter.getGroupId()))
                .slug(chapter.getSlug()).title(chapter.getTitle()).summary(chapter.getSummary())
                .bodyMarkdown(chapter.getBodyMarkdown()).sortOrder(chapter.getSortOrder())
                .status(chapter.getStatus()).updatedAt(chapter.getUpdatedAt()).build();
    }

    @Override
    public List<TutorialCategoryVO> categories() {
        return categoryRows().stream().map(this::categoryView).toList();
    }

    @Override
    @Transactional
    public TutorialCategoryVO createCategory(String name) {
        String validName = text(name, "知识体系名称", 100);
        String base = TutorialSlugDeriver.fromTitle(validName, "category");
        TutorialCategoryEntity category = new TutorialCategoryEntity();
        category.setName(validName);
        category.setSortOrder(nextOrder(categoryRows().stream().map(TutorialCategoryEntity::getSortOrder).toList()));
        for (int attempt = 1; attempt <= 8; attempt++) {
            String slug = attempt == 1 ? base : base + "-" + attempt;
            if (categoryMapper.selectCount(new LambdaQueryWrapper<TutorialCategoryEntity>()
                    .eq(TutorialCategoryEntity::getSlug, slug)) > 0) continue;
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

    @Override
    @Transactional
    public TutorialCategoryVO updateCategory(Long id, String name) {
        TutorialCategoryEntity category = requireCategory(id);
        category.setName(text(name, "知识体系名称", 100));
        category.setUpdatedAt(now());
        // 公开地址保持稳定：改名不会破坏已有链接。
        categoryMapper.updateById(category);
        return categoryView(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        requireCategory(id);
        if (tutorialMapper.selectCount(new LambdaQueryWrapper<TutorialEntity>()
                .eq(TutorialEntity::getCategoryId, id)) > 0) {
            throw new TutorialStateException("知识体系仍有教程，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void reorderCategories(List<Long> ids) {
        List<TutorialCategoryEntity> rows = categoryRows();
        requireExactOrder(ids, rows.stream().map(TutorialCategoryEntity::getId).toList());
        for (int index = 0; index < ids.size(); index++) {
            final Long id = ids.get(index);
            TutorialCategoryEntity row = rows.stream().filter(item -> item.getId().equals(id))
                    .findFirst().orElseThrow();
            row.setSortOrder((index + 1) * 10);
            row.setUpdatedAt(now());
            categoryMapper.updateById(row);
        }
    }

    @Override
    public PageResult<TutorialAdminVO> tutorials(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new TutorialInvalidRequestException("分页参数不合法");
        }
        List<TutorialEntity> rows = tutorialRows(null);
        long start = (long) (page - 1) * pageSize;
        List<TutorialAdminVO> items = start >= rows.size() ? List.of()
                : rows.subList((int) start, Math.min(rows.size(), (int) start + pageSize))
                    .stream().map(this::tutorialView).toList();
        return PageResult.<TutorialAdminVO>builder().items(items).total(rows.size())
                .page(page).pageSize(pageSize).build();
    }

    @Override
    public TutorialAdminVO tutorial(Long id) {
        return tutorialView(requireTutorial(id));
    }

    @Override
    @Transactional
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
            if (tutorialMapper.selectCount(new LambdaQueryWrapper<TutorialEntity>()
                    .eq(TutorialEntity::getSlug, slug)) > 0) continue;
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

    @Override
    @Transactional
    public TutorialAdminVO updateTutorial(Long id, TutorialUpdateDTO request) {
        TutorialEntity tutorial = lockTutorial(id);
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
        tutorialMapper.updateById(tutorial);
        return tutorialView(tutorial);
    }

    @Override
    @Transactional
    public void deleteTutorial(Long id) {
        TutorialEntity tutorial = lockTutorial(id);
        if (!"NEVER_PUBLISHED".equals(tutorial.getPublicationStatus())
                || groupMapper.selectCount(new LambdaQueryWrapper<TutorialGroupEntity>()
                        .eq(TutorialGroupEntity::getTutorialId, id)) > 0) {
            throw new TutorialStateException("只有未发布且没有分组的教程可以删除");
        }
        tutorialMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void reorderTutorials(Long categoryId, List<Long> ids) {
        requireCategory(categoryId);
        List<TutorialEntity> rows = tutorialRows(categoryId);
        requireExactOrder(ids, rows.stream().map(TutorialEntity::getId).toList());
        for (int index = 0; index < ids.size(); index++) {
            final Long id = ids.get(index);
            TutorialEntity row = rows.stream().filter(item -> item.getId().equals(id)).findFirst().orElseThrow();
            row.setSortOrder((index + 1) * 10);
            row.setUpdatedAt(now());
            tutorialMapper.updateById(row);
        }
    }

    @Override
    public TutorialCurriculumVO curriculum(Long tutorialId) {
        TutorialEntity tutorial = requireTutorial(tutorialId);
        List<TutorialGroupVO> groups = groupRows(tutorialId).stream()
                .map(group -> groupView(group, chapterRows(group.getId()))).toList();
        return TutorialCurriculumVO.builder().tutorial(tutorialView(tutorial)).groups(groups).build();
    }

    @Override
    @Transactional
    public TutorialGroupVO createGroup(Long tutorialId, String title) {
        lockTutorial(tutorialId);
        TutorialGroupEntity group = new TutorialGroupEntity();
        group.setTutorialId(tutorialId);
        group.setTitle(text(title, "分组名称", 200));
        group.setSortOrder(nextOrder(groupRows(tutorialId).stream()
                .map(TutorialGroupEntity::getSortOrder).toList()));
        group.setStatus("ACTIVE");
        groupMapper.insert(group);
        return groupView(group, List.of());
    }

    @Override
    @Transactional
    public TutorialGroupVO updateGroup(Long groupId, String title) {
        TutorialGroupEntity group = requireGroup(groupId);
        group.setTitle(text(title, "分组名称", 200));
        group.setUpdatedAt(now());
        groupMapper.updateById(group);
        return groupView(group, chapterRows(groupId));
    }

    @Override
    @Transactional
    public void archiveGroup(Long groupId) {
        TutorialGroupEntity group = requireGroup(groupId);
        long activeChapters = chapterMapper.selectCount(new LambdaQueryWrapper<TutorialChapterEntity>()
                .eq(TutorialChapterEntity::getGroupId, groupId)
                .eq(TutorialChapterEntity::getStatus, "ACTIVE"));
        if (activeChapters > 0) throw new TutorialGroupNotEmptyException();
        if (!"ACTIVE".equals(group.getStatus())) throw new TutorialStateException("分组已经归档");
        group.setStatus("ARCHIVED");
        group.setUpdatedAt(now());
        groupMapper.updateById(group);
    }

    @Override
    @Transactional
    public void restoreGroup(Long groupId) {
        TutorialGroupEntity group = requireGroup(groupId);
        if (!"ARCHIVED".equals(group.getStatus())) throw new TutorialStateException("只有已归档分组可以恢复");
        group.setStatus("ACTIVE");
        group.setUpdatedAt(now());
        groupMapper.updateById(group);
    }

    @Override
    @Transactional
    public void reorderGroups(Long tutorialId, List<Long> ids) {
        lockTutorial(tutorialId);
        List<TutorialGroupEntity> rows = groupRows(tutorialId);
        requireExactOrder(ids, rows.stream().map(TutorialGroupEntity::getId).toList());
        for (int index = 0; index < ids.size(); index++) {
            final Long id = ids.get(index);
            TutorialGroupEntity row = rows.stream().filter(item -> item.getId().equals(id))
                    .findFirst().orElseThrow();
            row.setSortOrder((index + 1) * 10);
            row.setUpdatedAt(now());
            groupMapper.updateById(row);
        }
    }

    @Override
    public TutorialChapterVO chapter(Long chapterId) {
        return chapterView(requireChapter(chapterId));
    }

    @Override
    @Transactional
    public TutorialChapterVO createChapter(Long groupId, ChapterCreateDTO request) {
        TutorialGroupEntity group = requireGroup(groupId);
        if (!"ACTIVE".equals(group.getStatus())) throw new TutorialStateException("不能在已归档分组中新建章节");
        lockTutorial(group.getTutorialId());
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
        chapter.setStatus("ACTIVE");
        for (int attempt = 1; attempt <= 8; attempt++) {
            String slug = attempt == 1 ? base : base + "-" + attempt;
            if (chapterMapper.selectCount(new LambdaQueryWrapper<TutorialChapterEntity>()
                    .eq(TutorialChapterEntity::getTutorialId, group.getTutorialId())
                    .eq(TutorialChapterEntity::getSlug, slug)) > 0) continue;
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

    @Override
    @Transactional
    public TutorialChapterVO updateChapter(Long chapterId, ChapterUpdateDTO request) {
        TutorialChapterEntity chapter = requireChapter(chapterId);
        if (request.getTitle() != null) chapter.setTitle(text(request.getTitle(), "章节标题", 200));
        chapter.setSummary(optionalText(request.getSummary(), 1000));
        chapter.setUpdatedAt(now());
        chapterMapper.updateById(chapter);
        return chapterView(chapter);
    }

    @Override
    @Transactional
    public TutorialChapterVO updateChapterBody(Long chapterId, ChapterBodyDTO request) {
        TutorialChapterEntity chapter = requireChapter(chapterId);
        chapter.setBodyMarkdown(bodyText(request.getBodyMarkdown()));
        chapter.setUpdatedAt(now());
        chapterMapper.updateById(chapter);
        mediaReferences.replaceChapter(chapterId, chapter.getBodyMarkdown());
        return chapterView(chapter);
    }

    @Override
    @Transactional
    public void archiveChapter(Long chapterId) {
        TutorialChapterEntity chapter = requireChapter(chapterId);
        if (!"ACTIVE".equals(chapter.getStatus())) throw new TutorialStateException("章节已经归档");
        chapter.setStatus("ARCHIVED");
        chapter.setUpdatedAt(now());
        chapterMapper.updateById(chapter);
    }

    @Override
    @Transactional
    public void restoreChapter(Long chapterId) {
        TutorialChapterEntity chapter = requireChapter(chapterId);
        TutorialGroupEntity group = requireGroup(chapter.getGroupId());
        if (!"ARCHIVED".equals(chapter.getStatus()) || !"ACTIVE".equals(group.getStatus())) {
            throw new TutorialStateException("只有使用中分组里的已归档章节可以恢复");
        }
        chapter.setStatus("ACTIVE");
        chapter.setUpdatedAt(now());
        chapterMapper.updateById(chapter);
    }

    @Override
    @Transactional
    public void moveChapter(Long chapterId, Long groupId) {
        TutorialChapterEntity chapter = requireChapter(chapterId);
        TutorialGroupEntity target = requireGroup(groupId);
        if (!chapter.getTutorialId().equals(target.getTutorialId()) || !"ACTIVE".equals(target.getStatus())) {
            throw new TutorialInvalidRequestException("目标分组必须属于同一教程且处于使用中");
        }
        if (groupId.equals(chapter.getGroupId())) return;
        lockTutorial(chapter.getTutorialId());
        chapter.setGroupId(groupId);
        chapter.setSortOrder(nextOrder(chapterRows(groupId).stream()
                .map(TutorialChapterEntity::getSortOrder).toList()));
        chapter.setUpdatedAt(now());
        chapterMapper.updateById(chapter);
    }

    @Override
    @Transactional
    public void reorderChapters(Long groupId, List<Long> ids) {
        TutorialGroupEntity group = requireGroup(groupId);
        lockTutorial(group.getTutorialId());
        List<TutorialChapterEntity> rows = chapterRows(groupId);
        requireExactOrder(ids, rows.stream().map(TutorialChapterEntity::getId).toList());
        for (int index = 0; index < ids.size(); index++) {
            final Long id = ids.get(index);
            TutorialChapterEntity row = rows.stream().filter(item -> item.getId().equals(id))
                    .findFirst().orElseThrow();
            row.setSortOrder((index + 1) * 10);
            row.setUpdatedAt(now());
            chapterMapper.updateById(row);
        }
    }
}
