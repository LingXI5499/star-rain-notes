package com.starrainnotes.tutorial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.media.api.MediaAssetPort;
import com.starrainnotes.seo.SeoContentChange;
import com.starrainnotes.tutorial.dto.AdminTutorialDetailView;
import com.starrainnotes.tutorial.dto.CreateTutorialRequest;
import com.starrainnotes.tutorial.dto.MoveTutorialRequest;
import com.starrainnotes.tutorial.dto.UpdateTutorialRequest;
import com.starrainnotes.tutorial.entity.Tutorial;
import com.starrainnotes.tutorial.entity.TutorialNode;
import com.starrainnotes.tutorial.mapper.TutorialCategoryMapper;
import com.starrainnotes.tutorial.mapper.TutorialMapper;
import com.starrainnotes.tutorial.mapper.TutorialNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Write-side tutorial metadata, ordering and publication lifecycle. */
@Service
@RequiredArgsConstructor
public class TutorialCommandService {
    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";
    private static final String WITHDRAWN = "WITHDRAWN";
    private final TutorialMapper tutorialMapper;
    private final TutorialCategoryMapper categoryMapper;
    private final TutorialNodeMapper nodeMapper;
    private final TutorialQueryService queryService;
    private final MediaAssetPort media;

    @Transactional
    public AdminTutorialDetailView create(CreateTutorialRequest request) {
        requireCategory(request.categoryId());
        media.requireImageIfPresent(request.coverMediaId());
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> slugExists(candidate, null));
        assertSlugFree(slug, null);
        Tutorial tutorial = Tutorial.builder().categoryId(request.categoryId()).title(request.title()).slug(slug)
                .summary(request.summary()).coverMediaId(request.coverMediaId())
                .sortOrder(request.sortOrder() == null ? nextTutorialOrder(request.categoryId()) : request.sortOrder())
                .publishStatus(DRAFT).build();
        tutorialMapper.insert(tutorial);
        return queryService.adminDetail(tutorial.getId());
    }

    @Transactional
    @SeoContentChange(kind = "tutorial", pathPrefix = "/tutorials/")
    public AdminTutorialDetailView update(Long tutorialId, UpdateTutorialRequest request) {
        Tutorial tutorial = requireTutorial(tutorialId);
        requireCategory(request.categoryId());
        media.requireImageIfPresent(request.coverMediaId());
        String slug = NumericSlugGenerator.forUpdate(request.slug(), tutorial.getSlug());
        assertSlugFree(slug, tutorialId);
        Long previousCategoryId = tutorial.getCategoryId();
        boolean categoryChanged = !previousCategoryId.equals(request.categoryId());
        tutorial.setCategoryId(request.categoryId());
        tutorial.setTitle(request.title());
        tutorial.setSlug(slug);
        tutorial.setSummary(request.summary());
        tutorial.setCoverMediaId(request.coverMediaId());
        tutorial.setSortOrder(categoryChanged ? nextTutorialOrder(request.categoryId())
                : (request.sortOrder() == null ? tutorial.getSortOrder() : request.sortOrder()));
        tutorialMapper.updateById(tutorial);
        if (categoryChanged) normalizeTutorialOrder(loadCategoryTutorials(previousCategoryId));
        return queryService.adminDetail(tutorialId);
    }

    @Transactional
    @SeoContentChange(kind = "tutorial", pathPrefix = "/tutorials/")
    public void delete(Long tutorialId) {
        requireTutorial(tutorialId);
        Long nodes = nodeMapper.selectCount(new LambdaQueryWrapper<TutorialNode>().eq(TutorialNode::getTutorialId, tutorialId));
        if (nodes != null && nodes > 0) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TUTORIAL_HAS_NODES",
                "Tutorial has nodes", "A tutorial containing nodes cannot be deleted.");
        tutorialMapper.deleteById(tutorialId);
    }

    @Transactional
    public void move(Long tutorialId, MoveTutorialRequest request) {
        Tutorial tutorial = requireTutorial(tutorialId);
        List<Tutorial> siblings = loadCategoryTutorials(tutorial.getCategoryId());
        siblings.removeIf(item -> item.getId().equals(tutorialId));
        siblings.add(Math.min(Math.max(request.targetIndex(), 0), siblings.size()), tutorial);
        normalizeTutorialOrder(siblings);
    }

    @Transactional
    @SeoContentChange(kind = "tutorial", pathPrefix = "/tutorials/")
    public AdminTutorialDetailView publish(Long tutorialId) {
        Tutorial tutorial = requireTutorial(tutorialId);
        if (!PUBLISHED.equals(tutorial.getPublishStatus())) {
            if (tutorial.getPublishedAt() == null) tutorial.setPublishedAt(LocalDateTime.now(Clock.systemUTC()));
            tutorial.setPublishStatus(PUBLISHED);
            tutorialMapper.updateById(tutorial);
        }
        return queryService.adminDetail(tutorialId);
    }

    @Transactional
    @SeoContentChange(kind = "tutorial", pathPrefix = "/tutorials/")
    public AdminTutorialDetailView withdraw(Long tutorialId) {
        Tutorial tutorial = requireTutorial(tutorialId);
        if (DRAFT.equals(tutorial.getPublishStatus())) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_PUBLISH_TRANSITION", "Cannot withdraw a draft", "Only published tutorials can be withdrawn.");
        if (!WITHDRAWN.equals(tutorial.getPublishStatus())) { tutorial.setPublishStatus(WITHDRAWN); tutorialMapper.updateById(tutorial); }
        return queryService.adminDetail(tutorialId);
    }

    private Tutorial requireTutorial(Long tutorialId) {
        Tutorial tutorial = tutorialMapper.selectById(tutorialId);
        if (tutorial == null) throw new ApiException(HttpStatus.NOT_FOUND, "TUTORIAL_NOT_FOUND", "Tutorial not found", "The tutorial does not exist.");
        return tutorial;
    }
    private void requireCategory(Long categoryId) {
        if (categoryId == null || categoryMapper.selectById(categoryId) == null) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "TUTORIAL_CATEGORY_NOT_FOUND", "Tutorial category not found", "The referenced category does not exist.");
    }
    private List<Tutorial> loadCategoryTutorials(Long categoryId) { return tutorialMapper.selectList(new LambdaQueryWrapper<Tutorial>()
            .eq(Tutorial::getCategoryId, categoryId).orderByAsc(Tutorial::getSortOrder).orderByAsc(Tutorial::getId)); }
    private int nextTutorialOrder(Long categoryId) { return loadCategoryTutorials(categoryId).stream()
            .mapToInt(item -> item.getSortOrder() == null ? 0 : item.getSortOrder()).max().orElse(0) + 10; }
    private void normalizeTutorialOrder(List<Tutorial> tutorials) { for (int i = 0; i < tutorials.size(); i++) {
        Tutorial tutorial = tutorials.get(i); int order = (i + 1) * 10;
        if (!Integer.valueOf(order).equals(tutorial.getSortOrder())) { tutorial.setSortOrder(order); tutorialMapper.updateById(tutorial); }
    }}
    private void assertSlugFree(String slug, Long excludedId) { if (slugExists(slug, excludedId)) throw new ApiException(HttpStatus.CONFLICT,
            "SLUG_CONFLICT", "Slug already exists", "A tutorial with this slug already exists."); }
    private boolean slugExists(String slug, Long excludedId) { LambdaQueryWrapper<Tutorial> query = new LambdaQueryWrapper<Tutorial>().eq(Tutorial::getSlug, slug);
        if (excludedId != null) query.ne(Tutorial::getId, excludedId); Long count = tutorialMapper.selectCount(query); return count != null && count > 0; }
}
