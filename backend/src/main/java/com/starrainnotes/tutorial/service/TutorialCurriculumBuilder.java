package com.starrainnotes.tutorial.service;

import com.starrainnotes.tutorial.dto.CurriculumNodeView;
import com.starrainnotes.tutorial.dto.FirstChapterView;
import com.starrainnotes.tutorial.entity.TutorialNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds the public curriculum from the fixed GROUP -> CHAPTER model. */
final class TutorialCurriculumBuilder {

    private static final Comparator<TutorialNode> ORDER = Comparator
            .comparing(TutorialNode::getSortOrder)
            .thenComparing(TutorialNode::getId);

    private TutorialCurriculumBuilder() {
    }

    record Curriculum(List<CurriculumNodeView> roots, FirstChapterView firstChapter, long publishedChapterCount) {
    }

    static Curriculum build(List<TutorialNode> nodes) {
        List<TutorialNode> chapters = publicChaptersInPreorder(nodes);
        Map<Long, List<TutorialNode>> byGroup = chapters.stream()
                .collect(Collectors.groupingBy(TutorialNode::getParentId));
        List<CurriculumNodeView> groups = rootGroups(nodes).stream()
                .filter(group -> !byGroup.getOrDefault(group.getId(), List.of()).isEmpty())
                .map(group -> new CurriculumNodeView(group.getId(), "GROUP", group.getTitle(), null,
                        byGroup.get(group.getId()).stream().map(TutorialCurriculumBuilder::chapterView).toList()))
                .toList();
        FirstChapterView first = chapters.isEmpty() ? null
                : new FirstChapterView(chapters.getFirst().getId(), chapters.getFirst().getTitle(),
                chapters.getFirst().getSlug());
        return new Curriculum(groups, first, chapters.size());
    }

    static List<TutorialNode> publicChaptersInPreorder(List<TutorialNode> nodes) {
        Map<Long, List<TutorialNode>> byGroup = publishedChapters(nodes).stream()
                .collect(Collectors.groupingBy(TutorialNode::getParentId));
        List<TutorialNode> ordered = new ArrayList<>();
        for (TutorialNode group : rootGroups(nodes)) {
            ordered.addAll(byGroup.getOrDefault(group.getId(), List.of()));
        }
        return ordered;
    }

    private static List<TutorialNode> rootGroups(List<TutorialNode> nodes) {
        return nodes.stream()
                .filter(node -> "GROUP".equals(node.getNodeType()) && node.getParentId() == null)
                .sorted(ORDER)
                .toList();
    }

    private static List<TutorialNode> publishedChapters(List<TutorialNode> nodes) {
        return nodes.stream()
                .filter(node -> "CHAPTER".equals(node.getNodeType())
                        && node.getParentId() != null
                        && "PUBLISHED".equals(node.getPublishStatus()))
                .sorted(ORDER)
                .toList();
    }

    private static CurriculumNodeView chapterView(TutorialNode chapter) {
        return new CurriculumNodeView(chapter.getId(), "CHAPTER", chapter.getTitle(), chapter.getSlug(), List.of());
    }
}
