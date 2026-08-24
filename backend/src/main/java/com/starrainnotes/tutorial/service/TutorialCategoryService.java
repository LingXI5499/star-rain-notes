package com.starrainnotes.tutorial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.tutorial.dto.CategoryNodeView;
import com.starrainnotes.tutorial.dto.CreateCategoryRequest;
import com.starrainnotes.tutorial.dto.MoveCategoryRequest;
import com.starrainnotes.tutorial.dto.PublicCategoryNodeView;
import com.starrainnotes.tutorial.dto.UpdateCategoryRequest;
import com.starrainnotes.tutorial.entity.Tutorial;
import com.starrainnotes.tutorial.entity.TutorialCategory;
import com.starrainnotes.tutorial.mapper.TutorialCategoryMapper;
import com.starrainnotes.tutorial.mapper.TutorialMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tutorial category management: admin tree, create/update/delete with cycle
 * protection and delete guards; public tree keeping only branches with at
 * least one published tutorial (04 §10).
 */
@Service
public class TutorialCategoryService {

    private final TutorialCategoryMapper categoryMapper;
    private final TutorialMapper tutorialMapper;

    public TutorialCategoryService(TutorialCategoryMapper categoryMapper, TutorialMapper tutorialMapper) {
        this.categoryMapper = categoryMapper;
        this.tutorialMapper = tutorialMapper;
    }

    // ---------------------------------------------------------------
    // trees
    // ---------------------------------------------------------------

    public List<CategoryNodeView> adminTree() {
        List<TutorialCategory> all = loadAll();
        return buildAdminTree(all);
    }

    public List<PublicCategoryNodeView> publicTree() {
        List<TutorialCategory> all = loadAll();
        if (all.isEmpty()) {
            return List.of();
        }
        Map<Long, List<TutorialCategory>> children = orderedChildren(all);
        List<Tutorial> published = tutorialMapper.selectList(
                new LambdaQueryWrapper<Tutorial>().eq(Tutorial::getPublishStatus, "PUBLISHED"));
        Map<Long, Long> counts = published.stream().collect(Collectors.groupingBy(
                Tutorial::getCategoryId, Collectors.counting()));

        List<PublicCategoryNodeView> roots = new ArrayList<>();
        for (TutorialCategory category : all) {
            if (category.getParentId() == null) {
                PublicCategoryNodeView pruned = prune(category, children, counts);
                if (pruned != null) {
                    roots.add(pruned);
                }
            }
        }
        return roots;
    }

    private PublicCategoryNodeView prune(TutorialCategory node,
                                         Map<Long, List<TutorialCategory>> children,
                                         Map<Long, Long> counts) {
        List<PublicCategoryNodeView> keptChildren = new ArrayList<>();
        for (TutorialCategory child : children.getOrDefault(node.getId(), List.of())) {
            PublicCategoryNodeView pruned = prune(child, children, counts);
            if (pruned != null) {
                keptChildren.add(pruned);
            }
        }
        long ownPublished = counts.getOrDefault(node.getId(), 0L);
        if (ownPublished > 0 || !keptChildren.isEmpty()) {
            return new PublicCategoryNodeView(node.getId(), node.getName(), node.getSlug(), keptChildren);
        }
        return null;
    }

    // ---------------------------------------------------------------
    // create / update / delete
    // ---------------------------------------------------------------

    @Transactional
    public CategoryNodeView create(CreateCategoryRequest request) {
        if (request.parentId() != null && categoryMapper.selectById(request.parentId()) == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PARENT_CATEGORY_NOT_FOUND",
                    "Parent category not found", "The referenced parent category does not exist.");
        }
        assertSlugFree(request.slug(), null);

        TutorialCategory category = new TutorialCategory();
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setParentId(request.parentId());
        category.setSortOrder(request.sortOrder() == null
                ? nextSiblingOrder(request.parentId()) : request.sortOrder());
        categoryMapper.insert(category);
        return toAdminNode(category, List.of());
    }

    @Transactional
    public CategoryNodeView update(Long categoryId, UpdateCategoryRequest request) {
        TutorialCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND",
                    "Category not found", "The category does not exist.");
        }
        assertNoCycle(categoryId, request.parentId());
        assertSlugFree(request.slug(), categoryId);

        Long oldParentId = category.getParentId();
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setParentId(request.parentId());
        category.setSortOrder(request.sortOrder() == null
                ? (java.util.Objects.equals(oldParentId, request.parentId())
                ? category.getSortOrder() : nextSiblingOrder(request.parentId()))
                : request.sortOrder());
        categoryMapper.updateById(category);
        if (!java.util.Objects.equals(oldParentId, request.parentId())) {
            normalizeSiblings(oldParentId);
            normalizeSiblings(request.parentId());
        }
        return toAdminNode(category, List.of());
    }

    /**
     * Reparents and reorders a category atomically.  This is deliberately a
     * small action endpoint so drag-and-drop does not need to resubmit title
     * or slug fields and cannot accidentally reset their sort order.
     */
    @Transactional
    public void move(Long categoryId, MoveCategoryRequest request) {
        TutorialCategory category = requireCategory(categoryId);
        assertNoCycle(categoryId, request.targetParentId());

        Long oldParentId = category.getParentId();
        Long newParentId = request.targetParentId();
        List<TutorialCategory> sourceSiblings = loadSiblings(oldParentId);
        List<TutorialCategory> targetSiblings = java.util.Objects.equals(oldParentId, newParentId)
                ? sourceSiblings : loadSiblings(newParentId);

        sourceSiblings.removeIf(item -> item.getId().equals(categoryId));
        if (targetSiblings != sourceSiblings) {
            targetSiblings.removeIf(item -> item.getId().equals(categoryId));
        }
        int index = Math.min(Math.max(request.targetIndex(), 0), targetSiblings.size());
        category.setParentId(newParentId);
        // Persist the reparent even when its existing sort order happens to
        // match the destination slot and therefore needs no normalization.
        categoryMapper.updateById(category);
        targetSiblings.add(index, category);

        normalizeSiblings(sourceSiblings);
        if (targetSiblings != sourceSiblings) {
            normalizeSiblings(targetSiblings);
        }
    }

    @Transactional
    public void delete(Long categoryId) {
        TutorialCategory category = requireCategory(categoryId);
        Long children = categoryMapper.selectCount(
                new LambdaQueryWrapper<TutorialCategory>().eq(TutorialCategory::getParentId, categoryId));
        if (children != null && children > 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CATEGORY_HAS_CHILDREN",
                    "Category has children", "A category with child categories cannot be deleted.");
        }
        Long tutorials = tutorialMapper.selectCount(
                new LambdaQueryWrapper<Tutorial>().eq(Tutorial::getCategoryId, categoryId));
        if (tutorials != null && tutorials > 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CATEGORY_HAS_TUTORIALS",
                    "Category has tutorials", "A category containing tutorials cannot be deleted.");
        }
        categoryMapper.deleteById(categoryId);
        normalizeSiblings(category.getParentId());
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private List<TutorialCategory> loadAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<TutorialCategory>()
                .orderByAsc(TutorialCategory::getSortOrder)
                .orderByAsc(TutorialCategory::getId));
    }

    private TutorialCategory requireCategory(Long categoryId) {
        TutorialCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND",
                    "Category not found", "The category does not exist.");
        }
        return category;
    }

    private List<TutorialCategory> loadSiblings(Long parentId) {
        LambdaQueryWrapper<TutorialCategory> wrapper = new LambdaQueryWrapper<TutorialCategory>()
                .orderByAsc(TutorialCategory::getSortOrder)
                .orderByAsc(TutorialCategory::getId);
        if (parentId == null) {
            wrapper.isNull(TutorialCategory::getParentId);
        } else {
            wrapper.eq(TutorialCategory::getParentId, parentId);
        }
        return categoryMapper.selectList(wrapper);
    }

    private int nextSiblingOrder(Long parentId) {
        return loadSiblings(parentId).stream()
                .mapToInt(item -> item.getSortOrder() == null ? 0 : item.getSortOrder())
                .max()
                .orElse(0) + 10;
    }

    private void normalizeSiblings(Long parentId) {
        normalizeSiblings(loadSiblings(parentId));
    }

    private void normalizeSiblings(List<TutorialCategory> siblings) {
        for (int index = 0; index < siblings.size(); index++) {
            TutorialCategory sibling = siblings.get(index);
            int order = (index + 1) * 10;
            if (!Integer.valueOf(order).equals(sibling.getSortOrder())) {
                sibling.setSortOrder(order);
                categoryMapper.updateById(sibling);
            }
        }
    }

    private List<CategoryNodeView> buildAdminTree(List<TutorialCategory> all) {
        Map<Long, CategoryNodeView> nodes = new LinkedHashMap<>();
        for (TutorialCategory category : all) {
            nodes.put(category.getId(), new CategoryNodeView(
                    category.getId(), category.getName(), category.getSlug(),
                    category.getSortOrder(), category.getParentId(), new ArrayList<>()));
        }
        List<CategoryNodeView> roots = new ArrayList<>();
        for (CategoryNodeView node : nodes.values()) {
            if (node.parentId() == null) {
                roots.add(node);
            } else {
                CategoryNodeView parent = nodes.get(node.parentId());
                if (parent != null) {
                    parent.children().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    private Map<Long, List<TutorialCategory>> orderedChildren(List<TutorialCategory> all) {
        Map<Long, List<TutorialCategory>> children = new HashMap<>();
        for (TutorialCategory category : all) {
            if (category.getParentId() != null) {
                children.computeIfAbsent(category.getParentId(), k -> new ArrayList<>()).add(category);
            }
        }
        return children;
    }

    private void assertSlugFree(String slug, Long excludeId) {
        LambdaQueryWrapper<TutorialCategory> wrapper =
                new LambdaQueryWrapper<TutorialCategory>().eq(TutorialCategory::getSlug, slug);
        if (excludeId != null) {
            wrapper.ne(TutorialCategory::getId, excludeId);
        }
        Long count = categoryMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SLUG_CONFLICT",
                    "Slug already exists", "A category with this slug already exists.");
        }
    }

    private void assertNoCycle(Long categoryId, Long newParentId) {
        if (newParentId == null) {
            return;
        }
        if (newParentId.equals(categoryId)) {
            throw cycle();
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = newParentId;
        while (cursor != null) {
            if (cursor.equals(categoryId)) {
                throw cycle();
            }
            if (!visited.add(cursor)) {
                return; // defensive; data should be cycle-free
            }
            TutorialCategory parent = categoryMapper.selectById(cursor);
            if (parent == null) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PARENT_CATEGORY_NOT_FOUND",
                        "Parent category not found", "The referenced parent category does not exist.");
            }
            cursor = parent.getParentId();
        }
    }

    private ApiException cycle() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CATEGORY_CYCLE",
                "Category cycle detected", "A category cannot be its own ancestor.");
    }

    private CategoryNodeView toAdminNode(TutorialCategory category, List<CategoryNodeView> children) {
        return new CategoryNodeView(category.getId(), category.getName(), category.getSlug(),
                category.getSortOrder(), category.getParentId(), children);
    }
}
