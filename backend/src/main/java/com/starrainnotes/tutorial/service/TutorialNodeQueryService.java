package com.starrainnotes.tutorial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.tutorial.assembler.TutorialNodeAssembler;
import com.starrainnotes.tutorial.dto.AdminCurriculumChapterView;
import com.starrainnotes.tutorial.dto.AdminCurriculumGroupView;
import com.starrainnotes.tutorial.dto.AdminCurriculumTutorialView;
import com.starrainnotes.tutorial.dto.AdminCurriculumView;
import com.starrainnotes.tutorial.dto.AdminTreeNodeView;
import com.starrainnotes.tutorial.dto.ChapterDetailView;
import com.starrainnotes.tutorial.entity.Tutorial;
import com.starrainnotes.tutorial.entity.TutorialCategory;
import com.starrainnotes.tutorial.entity.TutorialNode;
import com.starrainnotes.tutorial.mapper.TutorialCategoryMapper;
import com.starrainnotes.tutorial.mapper.TutorialMapper;
import com.starrainnotes.tutorial.mapper.TutorialNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Read-side tree, curriculum and chapter projections for tutorial administration. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TutorialNodeQueryService {
    private static final String GROUP = "GROUP";
    private static final String CHAPTER = "CHAPTER";
    private static final String PUBLISHED = "PUBLISHED";

    private final TutorialMapper tutorialMapper;
    private final TutorialCategoryMapper categoryMapper;
    private final TutorialNodeMapper nodeMapper;
    private final TutorialNodeAssembler nodeAssembler;

    public List<AdminTreeNodeView> tree(Long tutorialId) {
        requireTutorial(tutorialId);
        return TutorialTreeBuilder.build(loadAll(tutorialId));
    }

    public AdminCurriculumView curriculum(Long tutorialId) {
        Tutorial tutorial = requireTutorial(tutorialId);
        TutorialCategory category = categoryMapper.selectById(tutorial.getCategoryId());
        List<TutorialNode> all = loadAll(tutorialId);
        Map<Long, List<TutorialNode>> chapters = all.stream().filter(node -> CHAPTER.equals(node.getNodeType()))
                .collect(Collectors.groupingBy(TutorialNode::getParentId));
        List<AdminCurriculumGroupView> groups = all.stream().filter(node -> GROUP.equals(node.getNodeType()) && node.getParentId() == null)
                .map(group -> {
                    List<AdminCurriculumChapterView> rows = chapters.getOrDefault(group.getId(), List.of()).stream().map(nodeAssembler::toCurriculumChapter).toList();
                    long published = rows.stream().filter(row -> PUBLISHED.equals(row.publishStatus())).count();
                    return new AdminCurriculumGroupView(group.getId(), group.getTitle(), group.getSortOrder(), rows.size(), published, rows);
                }).toList();
        return new AdminCurriculumView(new AdminCurriculumTutorialView(tutorial.getId(), tutorial.getCategoryId(),
                category == null ? null : category.getName(), tutorial.getTitle(), tutorial.getSlug(), tutorial.getPublishStatus()), groups);
    }

    public ChapterDetailView chapter(Long tutorialId, Long chapterId) { return nodeAssembler.toChapterDetail(requireChapter(tutorialId, chapterId)); }
    public String chapterPublishStatus(Long tutorialId, Long chapterId) { return requireChapter(tutorialId, chapterId).getPublishStatus(); }

    private Tutorial requireTutorial(Long tutorialId) {
        Tutorial tutorial = tutorialMapper.selectById(tutorialId);
        if (tutorial == null) throw new ApiException(HttpStatus.NOT_FOUND, "TUTORIAL_NOT_FOUND", "Tutorial not found", "The tutorial does not exist.");
        return tutorial;
    }

    private TutorialNode requireChapter(Long tutorialId, Long chapterId) {
        TutorialNode node = nodeMapper.selectById(chapterId);
        if (node == null || !tutorialId.equals(node.getTutorialId())) throw new ApiException(HttpStatus.NOT_FOUND, "NODE_NOT_FOUND", "Node not found", "The node does not exist in this tutorial.");
        if (!CHAPTER.equals(node.getNodeType())) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "NODE_NOT_CHAPTER", "Wrong node type", "The node type does not match the operation.");
        if (node.getParentId() == null) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CHAPTER_GROUP_REQUIRED", "Chapter group required", "Every chapter must belong to a curriculum group.");
        return node;
    }

    private List<TutorialNode> loadAll(Long tutorialId) {
        return nodeMapper.selectList(new LambdaQueryWrapper<TutorialNode>().eq(TutorialNode::getTutorialId, tutorialId)
                .orderByAsc(TutorialNode::getSortOrder).orderByAsc(TutorialNode::getId));
    }

}
