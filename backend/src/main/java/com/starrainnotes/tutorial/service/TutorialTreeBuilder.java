package com.starrainnotes.tutorial.service;

import com.starrainnotes.tutorial.dto.AdminTreeNodeView;
import com.starrainnotes.tutorial.entity.TutorialNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple admin tree builder (04 §10): nests all nodes of one tutorial by
 * parentId, preserving the sortOrder/id preorder. Chapter body markdown is
 * intentionally excluded (only the chapter detail endpoint returns it).
 */
final class TutorialTreeBuilder {

    private TutorialTreeBuilder() {
    }

    static List<AdminTreeNodeView> build(List<TutorialNode> nodes) {
        Map<Long, AdminTreeNodeView> views = new LinkedHashMap<>();
        for (TutorialNode node : nodes) {
            views.put(node.getId(), new AdminTreeNodeView(
                    node.getId(), node.getParentId(), node.getNodeType(), node.getTitle(), node.getSlug(),
                    node.getPublishStatus(), node.getSortOrder(), new ArrayList<>()));
        }
        List<AdminTreeNodeView> roots = new ArrayList<>();
        for (TutorialNode node : nodes) {
            AdminTreeNodeView view = views.get(node.getId());
            if (node.getParentId() == null || !views.containsKey(node.getParentId())) {
                roots.add(view);
            } else {
                views.get(node.getParentId()).children().add(view);
            }
        }
        return roots;
    }
}
