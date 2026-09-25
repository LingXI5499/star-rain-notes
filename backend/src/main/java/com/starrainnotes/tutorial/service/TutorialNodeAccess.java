package com.starrainnotes.tutorial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.tutorial.entity.Tutorial;
import com.starrainnotes.tutorial.entity.TutorialNode;
import com.starrainnotes.tutorial.mapper.TutorialMapper;
import com.starrainnotes.tutorial.mapper.TutorialNodeMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/** Shared lookups and sort-order writes for the two-level curriculum. */
@Component
public class TutorialNodeAccess {
    static final String GROUP = "GROUP";
    static final String CHAPTER = "CHAPTER";

    private final TutorialMapper tutorialMapper;
    private final TutorialNodeMapper nodeMapper;

    public TutorialNodeAccess(TutorialMapper tutorialMapper, TutorialNodeMapper nodeMapper) {
        this.tutorialMapper = tutorialMapper;
        this.nodeMapper = nodeMapper;
    }

    public TutorialNodeMapper nodes() {
        return nodeMapper;
    }

    public Tutorial requireTutorial(Long tutorialId) {
        Tutorial tutorial = tutorialMapper.selectById(tutorialId);
        if (tutorial == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TUTORIAL_NOT_FOUND",
                    "Tutorial not found", "The tutorial does not exist.");
        }
        return tutorial;
    }

    public TutorialNode requireNode(Long tutorialId, Long nodeId) {
        TutorialNode node = nodeMapper.selectById(nodeId);
        if (node == null || !tutorialId.equals(node.getTutorialId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NODE_NOT_FOUND",
                    "Node not found", "The node does not exist in this tutorial.");
        }
        return node;
    }

    public TutorialNode requireGroup(Long tutorialId, Long groupId) {
        TutorialNode group = requireNode(tutorialId, groupId);
        requireType(group, GROUP, "NODE_NOT_GROUP");
        if (group.getParentId() != null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GROUP_NESTING_FORBIDDEN",
                    "Groups cannot be nested", "Curriculum groups must be root-level siblings.");
        }
        return group;
    }

    public TutorialNode requireTargetGroup(Long tutorialId, Long groupId) {
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

    public TutorialNode requireChapter(Long tutorialId, Long chapterId) {
        TutorialNode chapter = requireNode(tutorialId, chapterId);
        requireType(chapter, CHAPTER, "NODE_NOT_CHAPTER");
        if (chapter.getParentId() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CHAPTER_GROUP_REQUIRED",
                    "Chapter group required", "Every chapter must belong to a curriculum group.");
        }
        return chapter;
    }

    public void assertChapterSlugFree(Long tutorialId, String slug, Long excludeId) {
        if (chapterSlugExists(tutorialId, slug, excludeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "SLUG_CONFLICT",
                    "Slug already exists", "A chapter with this slug already exists in the tutorial.");
        }
    }

    public boolean chapterSlugExists(Long tutorialId, String slug, Long excludeId) {
        LambdaQueryWrapper<TutorialNode> wrapper = new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getTutorialId, tutorialId).eq(TutorialNode::getSlug, slug);
        if (excludeId != null) wrapper.ne(TutorialNode::getId, excludeId);
        Long count = nodeMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    public List<TutorialNode> loadGroups(Long tutorialId) {
        return nodeMapper.selectList(new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getTutorialId, tutorialId)
                .eq(TutorialNode::getNodeType, GROUP)
                .isNull(TutorialNode::getParentId)
                .orderByAsc(TutorialNode::getSortOrder).orderByAsc(TutorialNode::getId));
    }

    public List<TutorialNode> loadChapters(Long tutorialId, Long groupId) {
        return nodeMapper.selectList(new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getTutorialId, tutorialId)
                .eq(TutorialNode::getNodeType, CHAPTER)
                .eq(TutorialNode::getParentId, groupId)
                .orderByAsc(TutorialNode::getSortOrder).orderByAsc(TutorialNode::getId));
    }

    public int nextGroupOrder(Long tutorialId) {
        return loadGroups(tutorialId).stream().mapToInt(n -> n.getSortOrder() == null ? 0 : n.getSortOrder())
                .max().orElse(0) + 10;
    }

    public int nextChapterOrder(Long tutorialId, Long groupId) {
        return loadChapters(tutorialId, groupId).stream()
                .mapToInt(n -> n.getSortOrder() == null ? 0 : n.getSortOrder()).max().orElse(0) + 10;
    }

    public void normalizeOrders(List<TutorialNode> nodes) {
        for (int index = 0; index < nodes.size(); index++) {
            TutorialNode node = nodes.get(index);
            int order = (index + 1) * 10;
            if (!Integer.valueOf(order).equals(node.getSortOrder())) {
                node.setSortOrder(order);
                nodeMapper.updateById(node);
            }
        }
    }

    private void requireType(TutorialNode node, String expected, String code) {
        if (!expected.equals(node.getNodeType())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code,
                    "Wrong node type", "The node type does not match the operation.");
        }
    }
}
