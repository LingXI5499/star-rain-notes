package com.starrainnotes.tutorial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Flat knowledge-system management for the tutorial catalog. */
@Service
public class TutorialCategoryService {

    private final TutorialCategoryMapper categoryMapper;
    private final TutorialMapper tutorialMapper;

    public TutorialCategoryService(TutorialCategoryMapper categoryMapper, TutorialMapper tutorialMapper) {
        this.categoryMapper = categoryMapper;
        this.tutorialMapper = tutorialMapper;
    }

    public List<CategoryNodeView> adminTree() {
        return loadAll().stream().map(this::toAdminNode).toList();
    }

    public List<PublicCategoryNodeView> publicTree() {
        Map<Long, Long> counts = tutorialMapper.selectList(
                        new LambdaQueryWrapper<Tutorial>().eq(Tutorial::getPublishStatus, "PUBLISHED"))
                .stream().collect(Collectors.groupingBy(Tutorial::getCategoryId, Collectors.counting()));
        return loadAll().stream()
                .filter(category -> counts.getOrDefault(category.getId(), 0L) > 0)
                .map(category -> new PublicCategoryNodeView(
                        category.getId(), category.getName(), category.getSlug(), List.of()))
                .toList();
    }

    @Transactional
    public CategoryNodeView create(CreateCategoryRequest request) {
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> slugExists(candidate, null));
        assertSlugFree(slug, null);
        TutorialCategory category = new TutorialCategory();
        category.setName(request.name());
        category.setSlug(slug);
        category.setParentId(null);
        category.setSortOrder(request.sortOrder() == null ? nextOrder() : request.sortOrder());
        categoryMapper.insert(category);
        return toAdminNode(category);
    }

    @Transactional
    public CategoryNodeView update(Long categoryId, UpdateCategoryRequest request) {
        TutorialCategory category = requireCategory(categoryId);
        String slug = NumericSlugGenerator.forUpdate(request.slug(), category.getSlug());
        assertSlugFree(slug, categoryId);
        category.setName(request.name());
        category.setSlug(slug);
        category.setParentId(null);
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        categoryMapper.updateById(category);
        return toAdminNode(category);
    }

    @Transactional
    public void move(Long categoryId, MoveCategoryRequest request) {
        TutorialCategory category = requireCategory(categoryId);
        List<TutorialCategory> siblings = loadAll();
        siblings.removeIf(item -> item.getId().equals(categoryId));
        int index = Math.min(Math.max(request.targetIndex(), 0), siblings.size());
        siblings.add(index, category);
        normalize(siblings);
    }

    @Transactional
    public void delete(Long categoryId) {
        requireCategory(categoryId);
        Long tutorials = tutorialMapper.selectCount(
                new LambdaQueryWrapper<Tutorial>().eq(Tutorial::getCategoryId, categoryId));
        if (tutorials != null && tutorials > 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CATEGORY_HAS_TUTORIALS",
                    "Category has tutorials", "A knowledge system containing tutorials cannot be deleted.");
        }
        categoryMapper.deleteById(categoryId);
        normalize(loadAll());
    }

    private List<TutorialCategory> loadAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<TutorialCategory>()
                .isNull(TutorialCategory::getParentId)
                .orderByAsc(TutorialCategory::getSortOrder)
                .orderByAsc(TutorialCategory::getId));
    }

    private TutorialCategory requireCategory(Long categoryId) {
        TutorialCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND",
                    "Category not found", "The knowledge system does not exist.");
        }
        return category;
    }

    private int nextOrder() {
        return loadAll().stream().mapToInt(item -> item.getSortOrder() == null ? 0 : item.getSortOrder())
                .max().orElse(0) + 10;
    }

    private void normalize(List<TutorialCategory> categories) {
        for (int index = 0; index < categories.size(); index++) {
            TutorialCategory category = categories.get(index);
            int order = (index + 1) * 10;
            if (!Integer.valueOf(order).equals(category.getSortOrder())) {
                category.setSortOrder(order);
                categoryMapper.updateById(category);
            }
        }
    }

    private void assertSlugFree(String slug, Long excludeId) {
        LambdaQueryWrapper<TutorialCategory> wrapper =
                new LambdaQueryWrapper<TutorialCategory>().eq(TutorialCategory::getSlug, slug);
        if (excludeId != null) wrapper.ne(TutorialCategory::getId, excludeId);
        Long count = categoryMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SLUG_CONFLICT",
                    "Slug already exists", "A knowledge system with this slug already exists.");
        }
    }

    private boolean slugExists(String slug, Long excludeId) {
        LambdaQueryWrapper<TutorialCategory> wrapper =
                new LambdaQueryWrapper<TutorialCategory>().eq(TutorialCategory::getSlug, slug);
        if (excludeId != null) wrapper.ne(TutorialCategory::getId, excludeId);
        Long count = categoryMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private CategoryNodeView toAdminNode(TutorialCategory category) {
        return new CategoryNodeView(category.getId(), category.getName(), category.getSlug(),
                category.getSortOrder(), null, List.of());
    }
}
