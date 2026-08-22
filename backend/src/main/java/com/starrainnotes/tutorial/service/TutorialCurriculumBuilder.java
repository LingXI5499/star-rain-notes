package com.starrainnotes.tutorial.service;

import com.starrainnotes.tutorial.dto.CurriculumNodeView;
import com.starrainnotes.tutorial.dto.FirstChapterView;
import com.starrainnotes.tutorial.entity.TutorialNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the public curriculum tree from raw {@link TutorialNode} rows.
 *
 * <p>Rules (01 §2): all visible CHAPTERs are PUBLISHED; GROUPs are shown only
 * when they contain at least one published chapter descendant (a group with no
 * published descendant is hidden). Children are ordered by sortOrder then id.
 * The first chapter is the first PUBLISHED chapter in depth-first preorder,
 * which is also the reading order for Prev/Next.</p>
 */
final class TutorialCurriculumBuilder {

    private TutorialCurriculumBuilder() {
    }

    record Curriculum(List<CurriculumNodeView> roots, FirstChapterView firstChapter, long publishedChapterCount) {
    }

    static Curriculum build(List<TutorialNode> nodes) {
        List<TutorialNode> visible = visibleNodes(nodes);
        List<CurriculumNodeView> roots = buildTree(visible);
        pruneEmptyGroups(roots);
        long chapterCount = visible.stream()
                .filter(n -> "CHAPTER".equals(n.getNodeType()))
                .count();
        return new Curriculum(roots, firstChapter(roots), chapterCount);
    }

    /**
     * Flat list of PUBLISHED chapters in depth-first preorder — the reading
     * order used for Start Learning and Prev/Next (01 §2.4).
     */
    static List<TutorialNode> publicChaptersInPreorder(List<TutorialNode> nodes) {
        List<TutorialNode> visible = visibleNodes(nodes);
        Map<Long, TutorialNode> byId = visible.stream()
                .collect(Collectors.toMap(TutorialNode::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Long, List<Long>> children = new HashMap<>();
        for (TutorialNode node : visible) {
            if (node.getParentId() != null && byId.containsKey(node.getParentId())) {
                children.computeIfAbsent(node.getParentId(), k -> new ArrayList<>()).add(node.getId());
            }
        }
        List<TutorialNode> result = new ArrayList<>();
        for (TutorialNode node : visible) {
            if (node.getParentId() == null || !byId.containsKey(node.getParentId())) {
                collectPreorder(node, children, byId, result);
            }
        }
        return result;
    }

    private static List<TutorialNode> visibleNodes(List<TutorialNode> nodes) {
        return nodes.stream()
                .filter(n -> "GROUP".equals(n.getNodeType())
                        || "PUBLISHED".equals(n.getPublishStatus()))
                .sorted(Comparator.comparing(TutorialNode::getSortOrder)
                        .thenComparing(TutorialNode::getId))
                .toList();
    }

    private static List<CurriculumNodeView> buildTree(List<TutorialNode> visible) {
        Map<Long, CurriculumNodeView> views = new LinkedHashMap<>();
        for (TutorialNode node : visible) {
            views.put(node.getId(), new CurriculumNodeView(
                    node.getId(), node.getNodeType(), node.getTitle(), node.getSlug(), new ArrayList<>()));
        }
        List<CurriculumNodeView> roots = new ArrayList<>();
        for (TutorialNode node : visible) {
            CurriculumNodeView view = views.get(node.getId());
            if (node.getParentId() == null || !views.containsKey(node.getParentId())) {
                roots.add(view);
            } else {
                views.get(node.getParentId()).children().add(view);
            }
        }
        return roots;
    }

    private static boolean pruneEmptyGroups(List<CurriculumNodeView> roots) {
        boolean keptAny = false;
        List<CurriculumNodeView> kept = new ArrayList<>();
        for (CurriculumNodeView node : roots) {
            boolean childKept = pruneEmptyGroups(node.children());
            if ("CHAPTER".equals(node.type()) || childKept) {
                kept.add(node);
                keptAny = true;
            }
        }
        roots.clear();
        roots.addAll(kept);
        return keptAny;
    }

    private static void collectPreorder(TutorialNode node, Map<Long, List<Long>> children,
                                        Map<Long, TutorialNode> byId, List<TutorialNode> result) {
        if ("CHAPTER".equals(node.getNodeType())) {
            result.add(node);
        }
        for (Long childId : children.getOrDefault(node.getId(), List.of())) {
            collectPreorder(byId.get(childId), children, byId, result);
        }
    }

    private static FirstChapterView firstChapter(List<CurriculumNodeView> roots) {
        for (CurriculumNodeView root : roots) {
            FirstChapterView found = firstChapterIn(root);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static FirstChapterView firstChapterIn(CurriculumNodeView node) {
        if ("CHAPTER".equals(node.type())) {
            return new FirstChapterView(node.id(), node.title(), node.slug());
        }
        for (CurriculumNodeView child : node.children()) {
            FirstChapterView found = firstChapterIn(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
