package com.starrainnotes.tutorial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.tutorial.dto.AdminTreeNodeView;
import com.starrainnotes.tutorial.dto.ChapterDetailView;
import com.starrainnotes.tutorial.dto.CreateChapterRequest;
import com.starrainnotes.tutorial.dto.CreateGroupRequest;
import com.starrainnotes.tutorial.dto.MoveNodeRequest;
import com.starrainnotes.tutorial.dto.UpdateChapterRequest;
import com.starrainnotes.tutorial.dto.UpdateGroupRequest;
import com.starrainnotes.tutorial.entity.Tutorial;
import com.starrainnotes.tutorial.entity.TutorialNode;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tutorial node tree + chapter management (04 §10):
 *
 * <ul>
 *   <li>GROUP may contain GROUP/CHAPTER; CHAPTER is always a leaf</li>
 *   <li>parent must belong to the same tutorial</li>
 *   <li>move is transactional, re-normalizes sibling sortOrder (i+1)*10,
 *       targetIndex is 0-based and clamped</li>
 *   <li>a GROUP cannot move into itself or its own descendant</li>
 *   <li>chapter publish requires the tutorial to be PUBLISHED first</li>
 *   <li>first publish stamps publishedAt; withdraw/republish keep it</li>
 * </ul>
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
    private final TutorialNodeMapper nodeMapper;
    private final SiteSettingsTimezone siteSettingsTimezone;

    public TutorialNodeService(TutorialMapper tutorialMapper,
                               TutorialNodeMapper nodeMapper,
                               SiteSettingsTimezone siteSettingsTimezone) {
        this.tutorialMapper = tutorialMapper;
        this.nodeMapper = nodeMapper;
        this.siteSettingsTimezone = siteSettingsTimezone;
    }

    // ---------------------------------------------------------------
    // tree
    // ---------------------------------------------------------------

    public List<AdminTreeNodeView> tree(Long tutorialId) {
        requireTutorial(tutorialId);
        return TutorialTreeBuilder.build(loadAll(tutorialId));
    }

    // ---------------------------------------------------------------
    // groups
    // ---------------------------------------------------------------

    public AdminTreeNodeView createGroup(Long tutorialId, CreateGroupRequest request) {
        requireTutorial(tutorialId);
        Long parentId = validateParent(tutorialId, request.parentId());

        TutorialNode group = new TutorialNode();
        group.setTutorialId(tutorialId);
        group.setParentId(parentId);
        group.setNodeType(GROUP);
        group.setTitle(request.title());
        group.setSortOrder(nextSiblingOrder(tutorialId, parentId));
        nodeMapper.insert(group);
        return toNodeView(group);
    }

    public AdminTreeNodeView updateGroup(Long tutorialId, Long groupId, UpdateGroupRequest request) {
        TutorialNode group = requireNode(tutorialId, groupId);
        requireType(group, GROUP, "NODE_NOT_GROUP");
        group.setTitle(request.title());
        nodeMapper.updateById(group);
        return toNodeView(group);
    }

    public void deleteGroup(Long tutorialId, Long groupId) {
        TutorialNode group = requireNode(tutorialId, groupId);
        requireType(group, GROUP, "NODE_NOT_GROUP");
        Long children = nodeMapper.selectCount(
                new LambdaQueryWrapper<TutorialNode>().eq(TutorialNode::getParentId, groupId));
        if (children != null && children > 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GROUP_HAS_CHILDREN",
                    "Group has children", "A group containing nodes cannot be deleted.");
        }
        nodeMapper.deleteById(groupId);
    }

    // ---------------------------------------------------------------
    // chapters
    // ---------------------------------------------------------------

    public ChapterDetailView createChapter(Long tutorialId, CreateChapterRequest request) {
        requireTutorial(tutorialId);
        Long parentId = validateParent(tutorialId, request.parentId());
        assertChapterSlugFree(tutorialId, request.slug(), null);

        TutorialNode chapter = new TutorialNode();
        chapter.setTutorialId(tutorialId);
        chapter.setParentId(parentId);
        chapter.setNodeType(CHAPTER);
        chapter.setTitle(request.title());
        chapter.setSlug(request.slug());
        chapter.setSummary(request.summary());
        chapter.setBodyMarkdown(request.bodyMarkdown());
        chapter.setPublishStatus(DRAFT);
        chapter.setPublishedAt(null);
        chapter.setSortOrder(nextSiblingOrder(tutorialId, parentId));
        nodeMapper.insert(chapter);
        return toChapterDetail(chapter);
    }

    public ChapterDetailView getChapter(Long tutorialId, Long chapterId) {
        TutorialNode chapter = requireNode(tutorialId, chapterId);
        requireType(chapter, CHAPTER, "NODE_NOT_CHAPTER");
        return toChapterDetail(chapter);
    }

    public ChapterDetailView updateChapter(Long tutorialId, Long chapterId, UpdateChapterRequest request) {
        TutorialNode chapter = requireNode(tutorialId, chapterId);
        requireType(chapter, CHAPTER, "NODE_NOT_CHAPTER");
        assertChapterSlugFree(tutorialId, request.slug(), chapterId);

        chapter.setTitle(request.title());
        chapter.setSlug(request.slug());
        chapter.setSummary(request.summary());
        chapter.setBodyMarkdown(request.bodyMarkdown());
        // publishStatus / publishedAt are never touched by a plain update
        nodeMapper.updateById(chapter);
        return toChapterDetail(chapter);
    }

    public void deleteChapter(Long tutorialId, Long chapterId) {
        TutorialNode chapter = requireNode(tutorialId, chapterId);
        requireType(chapter, CHAPTER, "NODE_NOT_CHAPTER");
        nodeMapper.deleteById(chapterId);
    }

    public ChapterDetailView publishChapter(Long tutorialId, Long chapterId) {
        TutorialNode chapter = requireNode(tutorialId, chapterId);
        requireType(chapter, CHAPTER, "NODE_NOT_CHAPTER");
        requireTutorial(tutorialId);
        Tutorial tutorial = tutorialMapper.selectById(tutorialId);
        if (!PUBLISHED.equals(tutorial.getPublishStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TUTORIAL_NOT_PUBLISHED",
                    "Tutorial not published",
                    "Chapters can only be published while their tutorial is PUBLISHED.");
        }
        if (!PUBLISHED.equals(chapter.getPublishStatus())) {
            if (chapter.getPublishedAt() == null) {
                chapter.setPublishedAt(LocalDateTime.now(Clock.systemUTC()));
            }
            chapter.setPublishStatus(PUBLISHED);
            nodeMapper.updateById(chapter);
        }
        return toChapterDetail(chapter);
    }

    public ChapterDetailView withdrawChapter(Long tutorialId, Long chapterId) {
        TutorialNode chapter = requireNode(tutorialId, chapterId);
        requireType(chapter, CHAPTER, "NODE_NOT_CHAPTER");
        if (DRAFT.equals(chapter.getPublishStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PUBLISH_TRANSITION",
                    "Cannot withdraw a draft", "Only published chapters can be withdrawn.");
        }
        if (!WITHDRAWN.equals(chapter.getPublishStatus())) {
            // publishedAt is the FIRST public publication time and stays unchanged
            chapter.setPublishStatus(WITHDRAWN);
            nodeMapper.updateById(chapter);
        }
        return toChapterDetail(chapter);
    }

    // ---------------------------------------------------------------
    // move
    // ---------------------------------------------------------------

    @Transactional
    public void moveNode(Long tutorialId, Long nodeId, MoveNodeRequest request) {
        TutorialNode node = requireNode(tutorialId, nodeId);
        Long newParentId = request.targetParentId();
        if (newParentId != null) {
            TutorialNode parent = nodeMapper.selectById(newParentId);
            if (parent == null) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PARENT_NOT_FOUND",
                        "Target parent not found", "The target parent group does not exist.");
            }
            if (!tutorialId.equals(parent.getTutorialId())) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PARENT_CROSS_TUTORIAL",
                        "Target parent is in another tutorial",
                        "A node can only be moved within its own tutorial.");
            }
            if (!GROUP.equals(parent.getNodeType())) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PARENT_NOT_GROUP",
                        "Target parent must be a group",
                        "Chapters cannot be parents; only GROUPs or the root are valid targets.");
            }
            if (newParentId.equals(nodeId) || isDescendantOf(nodeId, newParentId)) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "NODE_CYCLE",
                        "Node cycle detected", "A node cannot be moved into itself or its own descendant.");
            }
        }

        List<TutorialNode> siblings = loadChildren(tutorialId, newParentId);
        siblings.removeIf(sibling -> sibling.getId().equals(nodeId));
        int index = Math.min(Math.max(request.targetIndex(), 0), siblings.size());
        siblings.add(index, node);

        // normalize sibling sortOrder to (i+1)*10 and persist within the transaction
        for (int i = 0; i < siblings.size(); i++) {
            TutorialNode sibling = siblings.get(i);
            int order = (i + 1) * 10;
            if (sibling.getId().equals(nodeId)) {
                sibling.setSortOrder(order);
                sibling.setParentId(newParentId);
                nodeMapper.updateById(sibling);
            } else if (!Integer.valueOf(order).equals(sibling.getSortOrder())) {
                sibling.setSortOrder(order);
                nodeMapper.updateById(sibling);
            }
        }
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

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

    private void requireType(TutorialNode node, String expected, String code) {
        if (!expected.equals(node.getNodeType())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code,
                    "Wrong node type", "The node type does not match the operation.");
        }
    }

    private Long validateParent(Long tutorialId, Long parentId) {
        if (parentId == null) {
            return null;
        }
        TutorialNode parent = nodeMapper.selectById(parentId);
        if (parent == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PARENT_NOT_FOUND",
                    "Parent not found", "The parent node does not exist.");
        }
        if (!tutorialId.equals(parent.getTutorialId())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PARENT_CROSS_TUTORIAL",
                    "Parent is in another tutorial",
                    "A node can only be created under a parent of the same tutorial.");
        }
        if (!GROUP.equals(parent.getNodeType())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PARENT_NOT_GROUP",
                    "Parent must be a group",
                    "Chapters cannot be parents; only GROUPs or the root are valid parents.");
        }
        return parentId;
    }

    private void assertChapterSlugFree(Long tutorialId, String slug, Long excludeId) {
        LambdaQueryWrapper<TutorialNode> wrapper = new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getTutorialId, tutorialId)
                .eq(TutorialNode::getSlug, slug);
        if (excludeId != null) {
            wrapper.ne(TutorialNode::getId, excludeId);
        }
        Long count = nodeMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SLUG_CONFLICT",
                    "Slug already exists", "A chapter with this slug already exists in the tutorial.");
        }
    }

    private boolean isDescendantOf(Long ancestorId, Long nodeId) {
        Set<Long> visited = new HashSet<>();
        Long cursor = nodeId;
        while (cursor != null) {
            if (cursor.equals(ancestorId)) {
                return true;
            }
            if (!visited.add(cursor)) {
                return false;
            }
            TutorialNode node = nodeMapper.selectById(cursor);
            if (node == null) {
                return false;
            }
            cursor = node.getParentId();
        }
        return false;
    }

    private List<TutorialNode> loadAll(Long tutorialId) {
        return nodeMapper.selectList(new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getTutorialId, tutorialId)
                .orderByAsc(TutorialNode::getSortOrder)
                .orderByAsc(TutorialNode::getId));
    }

    private List<TutorialNode> loadChildren(Long tutorialId, Long parentId) {
        LambdaQueryWrapper<TutorialNode> wrapper = new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getTutorialId, tutorialId);
        if (parentId == null) {
            wrapper.isNull(TutorialNode::getParentId);
        } else {
            wrapper.eq(TutorialNode::getParentId, parentId);
        }
        return nodeMapper.selectList(wrapper
                .orderByAsc(TutorialNode::getSortOrder)
                .orderByAsc(TutorialNode::getId));
    }

    private int nextSiblingOrder(Long tutorialId, Long parentId) {
        return loadChildren(tutorialId, parentId).stream()
                .mapToInt(n -> n.getSortOrder() == null ? 0 : n.getSortOrder())
                .max()
                .orElse(0) + 10;
    }

    private AdminTreeNodeView toNodeView(TutorialNode node) {
        return new AdminTreeNodeView(node.getId(), node.getParentId(), node.getNodeType(), node.getTitle(),
                node.getSlug(), node.getPublishStatus(), node.getSortOrder(), new ArrayList<>());
    }

    private ChapterDetailView toChapterDetail(TutorialNode chapter) {
        return new ChapterDetailView(
                chapter.getId(), chapter.getTutorialId(), chapter.getParentId(), chapter.getNodeType(),
                chapter.getTitle(), chapter.getSlug(), chapter.getSummary(), chapter.getBodyMarkdown(),
                chapter.getPublishStatus(), chapter.getSortOrder(),
                formatUtc(chapter.getPublishedAt()), formatUtc(chapter.getUpdatedAt()));
    }

    private String formatUtc(LocalDateTime utc) {
        if (utc == null) {
            return null;
        }
        return ZonedDateTime.of(utc, ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.of(siteSettingsTimezone.get()))
                .format(ISO_OFFSET);
    }
}
