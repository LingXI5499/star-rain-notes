package com.starrainnotes.tutorial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.tutorial.dto.CategoryNodeView;
import com.starrainnotes.tutorial.dto.PublicCategoryNodeView;
import com.starrainnotes.tutorial.entity.Tutorial;
import com.starrainnotes.tutorial.entity.TutorialCategory;
import com.starrainnotes.tutorial.mapper.TutorialCategoryMapper;
import com.starrainnotes.tutorial.mapper.TutorialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Read model for the flat tutorial-category catalogue. */
@Service
@RequiredArgsConstructor
public class TutorialCategoryQueryService {
    private final TutorialCategoryMapper categoryMapper;
    private final TutorialMapper tutorialMapper;

    public List<CategoryNodeView> adminTree() { return loadAll().stream().map(this::toAdminNode).toList(); }

    public List<PublicCategoryNodeView> publicTree() {
        Map<Long, Long> counts = tutorialMapper.selectList(new LambdaQueryWrapper<Tutorial>()
                        .eq(Tutorial::getPublishStatus, "PUBLISHED"))
                .stream().collect(Collectors.groupingBy(Tutorial::getCategoryId, Collectors.counting()));
        return loadAll().stream().filter(category -> counts.getOrDefault(category.getId(), 0L) > 0)
                .map(category -> new PublicCategoryNodeView(category.getId(), category.getName(), category.getSlug(), List.of()))
                .toList();
    }

    private List<TutorialCategory> loadAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<TutorialCategory>().isNull(TutorialCategory::getParentId)
                .orderByAsc(TutorialCategory::getSortOrder).orderByAsc(TutorialCategory::getId));
    }

    private CategoryNodeView toAdminNode(TutorialCategory category) {
        return new CategoryNodeView(category.getId(), category.getName(), category.getSlug(), category.getSortOrder(), null, List.of());
    }
}
