package com.starrainnotes.tutorial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.tutorial.dto.AdminCurriculumChapterView;
import com.starrainnotes.tutorial.dto.AdminCurriculumGroupView;
import com.starrainnotes.tutorial.dto.AdminCurriculumTutorialView;
import com.starrainnotes.tutorial.dto.AdminCurriculumView;
import com.starrainnotes.tutorial.dto.AdminTreeNodeView;
import com.starrainnotes.tutorial.dto.ChapterDetailView;
import com.starrainnotes.tutorial.dto.CreateChapterRequest;
import com.starrainnotes.tutorial.dto.CreateGroupRequest;
import com.starrainnotes.tutorial.dto.MoveIndexRequest;
import com.starrainnotes.tutorial.dto.MoveNodeRequest;
import com.starrainnotes.tutorial.dto.ReassignChapterRequest;
import com.starrainnotes.tutorial.dto.UpdateChapterRequest;
import com.starrainnotes.tutorial.dto.UpdateGroupRequest;
import com.starrainnotes.tutorial.entity.Tutorial;
import com.starrainnotes.tutorial.entity.TutorialCategory;
import com.starrainnotes.tutorial.entity.TutorialNode;
import com.starrainnotes.tutorial.mapper.TutorialCategoryMapper;
import com.starrainnotes.tutorial.mapper.TutorialMapper;
import com.starrainnotes.tutorial.mapper.TutorialNodeMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fixed two-level curriculum management.
 * GROUPs are root siblings; every CHAPTER belongs to exactly one GROUP.
 */
@Service
public class TutorialNodeService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String GROUP = "GROUP";
    private static final String CHAPTER = "CHAPTER";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";
    private static final String WITHDRAWN = "WITHDRAWN";

    private final TutorialMapper tutorialMapper;
    private final TutorialCategoryMapper categoryMapper;
    private final TutorialNodeMapper nodeMapper;
    private final SiteSettingsTimezone siteSettingsTimezone;

    public TutorialNodeService(TutorialMapper tutorialMapper,
                               TutorialCategoryMapper categoryMapper,
                               TutorialNodeMapper nodeMapper,
                               SiteSettingsTimezone siteSettingsTimezone) {
        this.tutorialMapper = tutorialMapper;
        this.categoryMapper = categoryMapper;
        this.nodeMapper = nodeMapper;
        this.siteSettingsTimezone = siteSettingsTimezone;
    }

    /** Compatibility tree endpoint; V5 guarantees exactly two levels. */
    public List<AdminTreeNodeView> tree(Long tutorialId) {
        requireTutorial(tutorialId);
        return TutorialTreeBuilder.build(loadAll(tutorialId));
    }

    public AdminCurriculumView curriculum(Long tutorialId) {
        Tutorial tutorial = requireTutorial(tutorialId);
        TutorialCategory category = categoryMapper.selectById(tutorial.getCategoryId());
        List<TutorialNode> all = loadAll(tutorialId);
        Map<Long, List<TutorialNode>> chapters = all.stream()
                .filter(node -> CHAPTER.equals(node.getNodeType()))
                .collect(Collectors.groupingBy(TutorialNode::getParentId));
        List<AdminCurriculumGroupView> groups = all.stream()
                .filter(node -> GROUP.equals(node.getNodeType()) && node.getParentId() == null)
                .map(group -> {
                    List<AdminCurriculumChapterView> rows = chapters.getOrDefault(group.getId(), List.of())
                            .stream().map(this::toCurriculumChapter).toList();
                    long published = rows.stream()
                            .filter(row -> PUBLISHED.equals(row.publishStatus())).count();
                    return new AdminCurriculumGroupView(group.getId(), group.getTitle(), group.getSortOrder(),
                            rows.size(), published, rows);
                }).toList();
        return new AdminCurriculumView(
                new AdminCurriculumTutorialView(tutorial.getId(), tutorial.getCategoryId(),
                        category == null ? null : category.getName(), tutorial.getTitle(), tutorial.getSlug(),
                        tutorial.getPublishStatus()),
                groups);
    }

    public AdminTreeNodeView createGroup(Long tutorialId, CreateGroupRequest request) {
        requireTutorial(tutorialId);
        TutorialNode group = new TutorialNode();
        group.setTutorialId(tutorialId);
        group.setParentId(null);
        group.setNodeType(GROUP);
        group.setTitle(request.title());
        group.setSortOrder(nextGroupOrder(tutorialId));
        nodeMapper.insert(group);
        return toNodeView(group);
    }

    public AdminTreeNodeView updateGroup(Long tutorialId, Long groupId, UpdateGroupRequest request) {
        TutorialNode group = requireGroup(tutorialId, groupId);
        group.setTitle(request.title());
        nodeMapper.updateById(group);
        return toNodeView(group);
    }

    @Transactional
    public void deleteGroup(Long tutorialId, Long groupId) {
        requireGroup(tutorialId, groupId);
        Long children = nodeMapper.selectCount(new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getParentId, groupId));
        if (children != null && children > 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GROUP_HAS_CHILDREN",
                    "Group has chapters", "A group containing chapters cannot be deleted.");
        }
        nodeMapper.deleteById(groupId);
        normalizeOrders(loadGroups(tutorialId));
    }

    public ChapterDetailView createChapter(Long tutorialId, CreateChapterRequest request) {
        requireTutorial(tutorialId);
        TutorialNode group = requireTargetGroup(tutorialId, request.groupId());
        String slug = NumericSlugGenerator.forCreate(request.slug(),
                candidate -> chapterSlugExists(tutorialId, candidate, null));
        assertChapterSlugFree(tutorialId, slug, null);
        TutorialNode chapter = new TutorialNode();
        chapter.setTutorialId(tutorialId);
        chapter.setParentId(group.getId());
        chapter.setNodeType(CHAPTER);
        chapter.setTitle(request.title());
        chapter.setSlug(slug);
        chapter.setSummary(request.summary());
        chapter.setBodyMarkdown(request.bodyMarkdown());
        chapter.setPublishStatus(DRAFT);
        chapter.setPublishedAt(null);
        chapter.setSortOrder(nextChapterOrder(tutorialId, group.getId()));
        nodeMapper.insert(chapter);
        return toChapterDetail(chapter);
    }

    public ChapterDetailView getChapter(Long tutorialId, Long chapterId) {
        return toChapterDetail(requireChapter(tutorialId, chapterId));
    }

    public String chapterPublishStatus(Long tutorialId, Long chapterId) {
        return requireChapter(tutorialId, chapterId).getPublishStatus();
    }

    public ChapterDetailView updateChapter(Long tutorialId, Long chapterId, UpdateChapterRequest request) {
        TutorialNode chapter = requireChapter(tutorialId, chapterId);
        String slug = NumericSlugGenerator.forUpdate(request.slug(), chapter.getSlug());
        assertChapterSlugFree(tutorialId, slug, chapterId);
        chapter.setTitle(request.title());
        chapter.setSlug(slug);
        chapter.setSummary(request.summary());
        chapter.setBodyMarkdown(request.bodyMarkdown());
        nodeMapper.updateById(chapter);
        return toChapterDetail(chapter);
    }

    @Transactional
    public void deleteChapter(Long tutorialId, Long chapterId) {
        TutorialNode chapter = requireChapter(tutorialId, chapterId);
        Long groupId = chapter.getParentId();
        nodeMapper.deleteById(chapterId);
        normalizeOrders(loadChapters(tutorialId, groupId));
    }

    public ChapterDetailView publishChapter(Long tutorialId, Long chapterId) {
        TutorialNode chapter = requireChapter(tutorialId, chapterId);
        Tutorial tutorial = requireTutorial(tutorialId);
        if (!PUBLISHED.equals(tutorial.getPublishStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TUTORIAL_NOT_PUBLISHED",
                    "Tutorial not published", "Chapters can only be published while their tutorial is PUBLISHED.");
        }
        if (!PUBLISHED.equals(chapter.getPublishStatus())) {
            if (chapter.getPublishedAt() == null) chapter.setPublishedAt(LocalDateTime.now(Clock.systemUTC()));
            chapter.setPublishStatus(PUBLISHED);
            nodeMapper.updateById(chapter);
        }
        return toChapterDetail(chapter);
    }

    public ChapterDetailView withdrawChapter(Long tutorialId, Long chapterId) {
        TutorialNode chapter = requireChapter(tutorialId, chapterId);
        if (DRAFT.equals(chapter.getPublishStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PUBLISH_TRANSITION",
                    "Cannot withdraw a draft", "Only published chapters can be withdrawn.");
        }
        if (!WITHDRAWN.equals(chapter.getPublishStatus())) {
            chapter.setPublishStatus(WITHDRAWN);
            nodeMapper.updateById(chapter);
        }
        return toChapterDetail(chapter);
    }

    @Transactional
    public void moveGroup(Long tutorialId, Long groupId, MoveIndexRequest request) {
        TutorialNode group = requireGroup(tutorialId, groupId);
        List<TutorialNode> groups = loadGroups(tutorialId);
        groups.removeIf(item -> item.getId().equals(groupId));
        groups.add(Math.min(Math.max(request.targetIndex(), 0), groups.size()), group);
        normalizeOrders(groups);
    }

    @Transactional
    public void moveChapter(Long tutorialId, Long chapterId, MoveIndexRequest request) {
        TutorialNode chapter = requireChapter(tutorialId, chapterId);
        List<TutorialNode> chapters = loadChapters(tutorialId, chapter.getParentId());
        chapters.removeIf(item -> item.getId().equals(chapterId));
        chapters.add(Math.min(Math.max(request.targetIndex(), 0), chapters.size()), chapter);
        normalizeOrders(chapters);
    }

    @Transactional
    public void reassignChapter(Long tutorialId, Long chapterId, ReassignChapterRequest request) {
        TutorialNode chapter = requireChapter(tutorialId, chapterId);
        TutorialNode targetGroup = requireTargetGroup(tutorialId, request.targetGroupId());
        Long sourceGroupId = chapter.getParentId();
        if (sourceGroupId.equals(targetGroup.getId())) return;

        List<TutorialNode> source = loadChapters(tutorialId, sourceGroupId);
        source.removeIf(item -> item.getId().equals(chapterId));
        normalizeOrders(source);

        List<TutorialNode> target = loadChapters(tutorialId, targetGroup.getId());
        chapter.setParentId(targetGroup.getId());
        chapter.setSortOrder((target.size() + 1) * 10);
        nodeMapper.updateById(chapter);
        target.add(chapter);
        normalizeOrders(target);
    }

    /**
     * Compatibility adapter for the old generic move route. It may only sort
     * root groups or chapters inside their current group.
     */
    @Transactional
    public void moveNode(Long tutorialId, Long nodeId, MoveNodeRequest request) {
        TutorialNode node = requireNode(tutorialId, nodeId);
        if (GROUP.equals(node.getNodeType())) {
            if (request.targetParentId() != null) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GROUP_NESTING_FORBIDDEN",
                        "Groups cannot be nested", "Curriculum groups are root-level siblings.");
            }
            moveGroup(tutorialId, nodeId, new MoveIndexRequest(request.targetIndex()));
            return;
        }
        if (!java.util.Objects.equals(node.getParentId(), request.targetParentId())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CROSS_GROUP_DRAG_FORBIDDEN",
                    "Cross-group drag is not allowed",
                    "Use the explicit chapter reassignment action to change a chapter's group.");
        }
        moveChapter(tutorialId, nodeId, new MoveIndexRequest(request.targetIndex()));
    }

    private Tutorial requireTutorial(Long tutorialId) {
        Tutorial tutorial = tutorialMapper.selectById(tutorialId);
        if (tutorial == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TUTORIAL_NOT_FOUND",
                    "Tutorial not found", "The tutorial does not exist.");
        }
        return tutorial;
    }

    private TutorialNode requireNode(Long tutorialId, Long nodeId) {
        TutorialNode node = nodeMapper.selectById(nodeId);
        if (node == null || !tutorialId.equals(node.getTutorialId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NODE_NOT_FOUND",
                    "Node not found", "The node does not exist in this tutorial.");
        }
        return node;
    }

    private TutorialNode requireGroup(Long tutorialId, Long groupId) {
        TutorialNode group = requireNode(tutorialId, groupId);
        requireType(group, GROUP, "NODE_NOT_GROUP");
        if (group.getParentId() != null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GROUP_NESTING_FORBIDDEN",
                    "Groups cannot be nested", "Curriculum groups must be root-level siblings.");
        }
        return group;
    }

    private TutorialNode requireTargetGroup(Long tutorialId, Long groupId) {
        TutorialNode group = nodeMapper.selectById(groupId);
        if (group == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PARENT_NOT_FOUND",
                    "Group not found", "The target curriculum group does not exist.");
        }
        if (!tutorialId.equals(group.getTutorialId())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PARENT_CROSS_TUTORIAL",
                    "Group belongs to another tutorial", "A chapter and its group must belong to the same tutorial.");
        }
        if (!GROUP.equals(group.getNodeType())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PARENT_NOT_GROUP",
                    "Target is not a group", "Every chapter must belong to a curriculum group.");
        }
        if (group.getParentId() != null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GROUP_NESTING_FORBIDDEN",
                    "Groups cannot be nested", "Curriculum groups must be root-level siblings.");
        }
        return group;
    }

    private TutorialNode requireChapter(Long tutorialId, Long chapterId) {
        TutorialNode chapter = requireNode(tutorialId, chapterId);
        requireType(chapter, CHAPTER, "NODE_NOT_CHAPTER");
        if (chapter.getParentId() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CHAPTER_GROUP_REQUIRED",
                    "Chapter group required", "Every chapter must belong to a curriculum group.");
        }
        return chapter;
    }

    private void requireType(TutorialNode node, String expected, String code) {
        if (!expected.equals(node.getNodeType())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code,
                    "Wrong node type", "The node type does not match the operation.");
        }
    }

    private void assertChapterSlugFree(Long tutorialId, String slug, Long excludeId) {
        LambdaQueryWrapper<TutorialNode> wrapper = new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getTutorialId, tutorialId).eq(TutorialNode::getSlug, slug);
        if (excludeId != null) wrapper.ne(TutorialNode::getId, excludeId);
        Long count = nodeMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SLUG_CONFLICT",
                    "Slug already exists", "A chapter with this slug already exists in the tutorial.");
        }
    }

    private boolean chapterSlugExists(Long tutorialId, String slug, Long excludeId) {
        LambdaQueryWrapper<TutorialNode> wrapper = new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getTutorialId, tutorialId).eq(TutorialNode::getSlug, slug);
        if (excludeId != null) wrapper.ne(TutorialNode::getId, excludeId);
        Long count = nodeMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private List<TutorialNode> loadAll(Long tutorialId) {
        return nodeMapper.selectList(new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getTutorialId, tutorialId)
                .orderByAsc(TutorialNode::getSortOrder).orderByAsc(TutorialNode::getId));
    }

    private List<TutorialNode> loadGroups(Long tutorialId) {
        return nodeMapper.selectList(new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getTutorialId, tutorialId)
                .eq(TutorialNode::getNodeType, GROUP)
                .isNull(TutorialNode::getParentId)
                .orderByAsc(TutorialNode::getSortOrder).orderByAsc(TutorialNode::getId));
    }

    private List<TutorialNode> loadChapters(Long tutorialId, Long groupId) {
        return nodeMapper.selectList(new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getTutorialId, tutorialId)
                .eq(TutorialNode::getNodeType, CHAPTER)
                .eq(TutorialNode::getParentId, groupId)
                .orderByAsc(TutorialNode::getSortOrder).orderByAsc(TutorialNode::getId));
    }

    private int nextGroupOrder(Long tutorialId) {
        return loadGroups(tutorialId).stream().mapToInt(n -> n.getSortOrder() == null ? 0 : n.getSortOrder())
                .max().orElse(0) + 10;
    }

    private int nextChapterOrder(Long tutorialId, Long groupId) {
        return loadChapters(tutorialId, groupId).stream()
                .mapToInt(n -> n.getSortOrder() == null ? 0 : n.getSortOrder()).max().orElse(0) + 10;
    }

    private void normalizeOrders(List<TutorialNode> nodes) {
        for (int index = 0; index < nodes.size(); index++) {
            TutorialNode node = nodes.get(index);
            int order = (index + 1) * 10;
            if (!Integer.valueOf(order).equals(node.getSortOrder())) {
                node.setSortOrder(order);
                nodeMapper.updateById(node);
            }
        }
    }

    private AdminTreeNodeView toNodeView(TutorialNode node) {
        return new AdminTreeNodeView(node.getId(), node.getParentId(), node.getNodeType(), node.getTitle(),
                node.getSlug(), node.getPublishStatus(), node.getSortOrder(), new ArrayList<>());
    }

    private AdminCurriculumChapterView toCurriculumChapter(TutorialNode chapter) {
        return new AdminCurriculumChapterView(chapter.getId(), chapter.getParentId(), chapter.getTitle(),
                chapter.getSlug(), chapter.getPublishStatus(), chapter.getSortOrder(), formatUtc(chapter.getUpdatedAt()));
    }

    private ChapterDetailView toChapterDetail(TutorialNode chapter) {
        return new ChapterDetailView(chapter.getId(), chapter.getTutorialId(), chapter.getParentId(),
                chapter.getNodeType(), chapter.getTitle(), chapter.getSlug(), chapter.getSummary(),
                chapter.getBodyMarkdown(), chapter.getPublishStatus(), chapter.getSortOrder(),
                formatUtc(chapter.getPublishedAt()), formatUtc(chapter.getUpdatedAt()));
    }

    private String formatUtc(LocalDateTime utc) {
        if (utc == null) return null;
        return ZonedDateTime.of(utc, ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.of(siteSettingsTimezone.get())).format(ISO_OFFSET);
    }
}
