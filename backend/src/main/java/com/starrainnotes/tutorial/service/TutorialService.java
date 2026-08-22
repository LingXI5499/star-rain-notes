package com.starrainnotes.tutorial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.tutorial.dto.AdminTutorialDetailView;
import com.starrainnotes.tutorial.dto.AdminTutorialSummaryView;
import com.starrainnotes.tutorial.dto.BreadcrumbView;
import com.starrainnotes.tutorial.dto.CategoryPathView;
import com.starrainnotes.tutorial.dto.CreateTutorialRequest;
import com.starrainnotes.tutorial.dto.CurriculumNodeView;
import com.starrainnotes.tutorial.dto.FirstChapterView;
import com.starrainnotes.tutorial.dto.PrevNextView;
import com.starrainnotes.tutorial.dto.PublicChapterView;
import com.starrainnotes.tutorial.dto.PublicTutorialDetailView;
import com.starrainnotes.tutorial.dto.PublicTutorialSummaryView;
import com.starrainnotes.tutorial.dto.TutorialPageView;
import com.starrainnotes.tutorial.dto.UpdateTutorialRequest;
import com.starrainnotes.tutorial.entity.Tutorial;
import com.starrainnotes.tutorial.entity.TutorialCategory;
import com.starrainnotes.tutorial.entity.TutorialNode;
import com.starrainnotes.tutorial.mapper.TutorialCategoryMapper;
import com.starrainnotes.tutorial.mapper.TutorialMapper;
import com.starrainnotes.tutorial.mapper.TutorialNodeMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tutorial management: admin CRUD + publish lifecycle (04 §10) and public
 * queries. PublishStatus/publishedAt are only changed by the /publish and
 * /withdraw actions; the first publish stamps publishedAt and withdraw /
 * republish keep it (AGENTS.md).
 */
@Service
public class TutorialService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";
    private static final String WITHDRAWN = "WITHDRAWN";

    private final TutorialMapper tutorialMapper;
    private final TutorialCategoryMapper categoryMapper;
    private final TutorialNodeMapper nodeMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final SiteSettingsTimezone siteSettingsTimezone;

    public TutorialService(TutorialMapper tutorialMapper,
                           TutorialCategoryMapper categoryMapper,
                           TutorialNodeMapper nodeMapper,
                           MediaAssetMapper mediaAssetMapper,
                           SiteSettingsTimezone siteSettingsTimezone) {
        this.tutorialMapper = tutorialMapper;
        this.categoryMapper = categoryMapper;
        this.nodeMapper = nodeMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.siteSettingsTimezone = siteSettingsTimezone;
    }

    // ---------------------------------------------------------------
    // admin
    // ---------------------------------------------------------------

    public TutorialPageView adminList(int page, int pageSize, String status, String query) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        LambdaQueryWrapper<Tutorial> wrapper = new LambdaQueryWrapper<Tutorial>()
                .eq(status != null && !status.isBlank(), Tutorial::getPublishStatus, status)
                .and(query != null && !query.isBlank(), w -> w
                        .like(Tutorial::getTitle, query).or().like(Tutorial::getSlug, query))
                .orderByDesc(Tutorial::getUpdatedAt)
                .orderByDesc(Tutorial::getId);

        Long total = tutorialMapper.selectCount(wrapper);
        int offset = (safePage - 1) * safeSize;
        wrapper.last("LIMIT " + safeSize + " OFFSET " + offset);
        List<Tutorial> rows = tutorialMapper.selectList(wrapper);

        Map<Long, String> categoryNames = categoryNameMap();
        List<AdminTutorialSummaryView> items = rows.stream()
                .map(t -> new AdminTutorialSummaryView(
                        t.getId(), t.getTitle(), t.getSlug(), t.getCategoryId(),
                        categoryNames.get(t.getCategoryId()), t.getPublishStatus(),
                        formatUtc(t.getPublishedAt()), formatUtc(t.getUpdatedAt())))
                .toList();

        long safeTotal = total == null ? 0 : total;
        int totalPages = safeTotal == 0 ? 0 : (int) ((safeTotal + safeSize - 1) / safeSize);
        return new TutorialPageView(items, safePage, safeSize, safeTotal, totalPages);
    }

    public AdminTutorialDetailView adminDetail(Long id) {
        Tutorial tutorial = requireTutorial(id);
        return toAdminDetail(tutorial);
    }

    public AdminTutorialDetailView create(CreateTutorialRequest request) {
        requireCategory(request.categoryId());
        assertSlugFree(request.slug(), null);

        Tutorial tutorial = new Tutorial();
        tutorial.setCategoryId(request.categoryId());
        tutorial.setTitle(request.title());
        tutorial.setSlug(request.slug());
        tutorial.setSummary(request.summary());
        tutorial.setCoverMediaId(request.coverMediaId());
        tutorial.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        tutorial.setSeoTitle(request.seoTitle());
        tutorial.setSeoDescription(request.seoDescription());
        tutorial.setPublishStatus(DRAFT);
        tutorial.setPublishedAt(null);
        tutorialMapper.insert(tutorial);
        return toAdminDetail(tutorial);
    }

    public AdminTutorialDetailView update(Long id, UpdateTutorialRequest request) {
        Tutorial tutorial = requireTutorial(id);
        requireCategory(request.categoryId());
        assertSlugFree(request.slug(), id);

        tutorial.setCategoryId(request.categoryId());
        tutorial.setTitle(request.title());
        tutorial.setSlug(request.slug());
        tutorial.setSummary(request.summary());
        tutorial.setCoverMediaId(request.coverMediaId());
        tutorial.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        tutorial.setSeoTitle(request.seoTitle());
        tutorial.setSeoDescription(request.seoDescription());
        // publishStatus / publishedAt are never touched by a plain update
        tutorialMapper.updateById(tutorial);
        return toAdminDetail(tutorial);
    }

    public void delete(Long id) {
        requireTutorial(id);
        Long nodes = nodeMapper.selectCount(
                new LambdaQueryWrapper<TutorialNode>().eq(TutorialNode::getTutorialId, id));
        if (nodes != null && nodes > 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TUTORIAL_HAS_NODES",
                    "Tutorial has nodes", "A tutorial containing nodes cannot be deleted.");
        }
        tutorialMapper.deleteById(id);
    }

    public AdminTutorialDetailView publish(Long id) {
        Tutorial tutorial = requireTutorial(id);
        if (!PUBLISHED.equals(tutorial.getPublishStatus())) {
            if (tutorial.getPublishedAt() == null) {
                tutorial.setPublishedAt(LocalDateTime.now(Clock.systemUTC()));
            }
            tutorial.setPublishStatus(PUBLISHED);
            tutorialMapper.updateById(tutorial);
        }
        return toAdminDetail(tutorial);
    }

    public AdminTutorialDetailView withdraw(Long id) {
        Tutorial tutorial = requireTutorial(id);
        if (DRAFT.equals(tutorial.getPublishStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PUBLISH_TRANSITION",
                    "Cannot withdraw a draft", "Only published tutorials can be withdrawn.");
        }
        if (!WITHDRAWN.equals(tutorial.getPublishStatus())) {
            // publishedAt is preserved: it is the FIRST public publication time
            tutorial.setPublishStatus(WITHDRAWN);
            tutorialMapper.updateById(tutorial);
        }
        return toAdminDetail(tutorial);
    }

    // ---------------------------------------------------------------
    // public
    // ---------------------------------------------------------------

    public List<PublicTutorialSummaryView> publicList(String categorySlug) {
        Set<Long> categoryIds = resolveCategoryFilter(categorySlug);

        List<Tutorial> rows = tutorialMapper.selectList(new LambdaQueryWrapper<Tutorial>()
                .in(Tutorial::getCategoryId, categoryIds)
                .eq(Tutorial::getPublishStatus, PUBLISHED)
                .orderByAsc(Tutorial::getSortOrder)
                .orderByAsc(Tutorial::getId));

        Map<Long, String> categoryNames = categoryNameMap();
        Map<Long, Long> chapterCounts = publishedChapterCounts();
        Map<Long, String> mediaUrls = coverUrlMap(
                rows.stream().map(Tutorial::getCoverMediaId).toList());
        // HashMap allows null cover values (Collectors.toMap would NPE)
        Map<Long, String> coverByTutorial = new HashMap<>();
        for (Tutorial t : rows) {
            coverByTutorial.put(t.getId(),
                    t.getCoverMediaId() == null ? null : mediaUrls.get(t.getCoverMediaId()));
        }

        return rows.stream()
                .map(t -> new PublicTutorialSummaryView(
                        t.getId(), t.getTitle(), t.getSlug(), t.getSummary(),
                        coverByTutorial.get(t.getId()), t.getCategoryId(),
                        categoryNames.get(t.getCategoryId()),
                        chapterCounts.getOrDefault(t.getId(), 0L)))
                .toList();
    }

    public PublicTutorialDetailView publicDetail(String slug) {
        Tutorial tutorial = tutorialMapper.selectOne(
                new LambdaQueryWrapper<Tutorial>().eq(Tutorial::getSlug, slug));
        if (tutorial == null || !PUBLISHED.equals(tutorial.getPublishStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TUTORIAL_NOT_FOUND",
                    "Tutorial not found", "The tutorial does not exist or is not public.");
        }

        List<CategoryPathView> path = categoryPath(tutorial.getCategoryId());
        List<TutorialNode> nodes = loadAllNodes(tutorial.getId());
        TutorialCurriculumBuilder.Curriculum curriculum = TutorialCurriculumBuilder.build(nodes);

        return new PublicTutorialDetailView(
                tutorial.getId(), tutorial.getTitle(), tutorial.getSlug(), tutorial.getSummary(),
                coverUrl(tutorial.getCoverMediaId()), path,
                curriculum.publishedChapterCount(), curriculum.firstChapter(), curriculum.roots(),
                tutorial.getSeoTitle(), tutorial.getSeoDescription(),
                formatUtc(tutorial.getPublishedAt()), formatUtc(tutorial.getUpdatedAt()));
    }

    public PublicChapterView publicChapter(String tutorialSlug, String chapterSlug) {
        Tutorial tutorial = tutorialMapper.selectOne(
                new LambdaQueryWrapper<Tutorial>().eq(Tutorial::getSlug, tutorialSlug));
        if (tutorial == null || !PUBLISHED.equals(tutorial.getPublishStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TUTORIAL_NOT_FOUND",
                    "Tutorial not found", "The tutorial does not exist or is not public.");
        }

        List<TutorialNode> nodes = loadAllNodes(tutorial.getId());
        Map<Long, TutorialNode> byId = nodes.stream()
                .collect(Collectors.toMap(TutorialNode::getId, Function.identity()));
        TutorialNode chapter = nodes.stream()
                .filter(n -> "CHAPTER".equals(n.getNodeType()) && chapterSlug.equals(n.getSlug()))
                .findFirst()
                .orElse(null);
        if (chapter == null || !PUBLISHED.equals(chapter.getPublishStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CHAPTER_NOT_FOUND",
                    "Chapter not found", "The chapter does not exist or is not public.");
        }

        List<BreadcrumbView> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(new BreadcrumbView("TUTORIAL", tutorial.getId(), tutorial.getTitle(), tutorial.getSlug()));
        List<TutorialNode> ancestors = new ArrayList<>();
        Long cursor = chapter.getParentId();
        while (cursor != null) {
            TutorialNode ancestor = byId.get(cursor);
            if (ancestor == null) {
                break;
            }
            ancestors.add(0, ancestor);
            cursor = ancestor.getParentId();
        }
        for (TutorialNode group : ancestors) {
            breadcrumbs.add(new BreadcrumbView("GROUP", group.getId(), group.getTitle(), null));
        }
        breadcrumbs.add(new BreadcrumbView("CHAPTER", chapter.getId(), chapter.getTitle(), chapter.getSlug()));

        List<TutorialNode> preorder = TutorialCurriculumBuilder.publicChaptersInPreorder(nodes);
        int index = -1;
        for (int i = 0; i < preorder.size(); i++) {
            if (preorder.get(i).getId().equals(chapter.getId())) {
                index = i;
                break;
            }
        }
        PrevNextView previous = index > 0 ? toPrevNext(preorder.get(index - 1)) : null;
        PrevNextView next = index >= 0 && index < preorder.size() - 1 ? toPrevNext(preorder.get(index + 1)) : null;

        return new PublicChapterView(
                chapter.getId(), chapter.getSlug(), chapter.getTitle(),
                tutorial.getId(), tutorial.getSlug(), tutorial.getTitle(), tutorial.getSummary(),
                chapter.getBodyMarkdown(), chapter.getSummary(),
                breadcrumbs, previous, next,
                formatUtc(chapter.getPublishedAt()), formatUtc(chapter.getUpdatedAt()));
    }

    private PrevNextView toPrevNext(TutorialNode chapter) {
        return new PrevNextView(chapter.getId(), chapter.getSlug(), chapter.getTitle());
    }

    private List<TutorialNode> loadAllNodes(Long tutorialId) {
        return nodeMapper.selectList(new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getTutorialId, tutorialId)
                .orderByAsc(TutorialNode::getSortOrder)
                .orderByAsc(TutorialNode::getId));
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Tutorial requireTutorial(Long id) {
        Tutorial tutorial = tutorialMapper.selectById(id);
        if (tutorial == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TUTORIAL_NOT_FOUND",
                    "Tutorial not found", "The tutorial does not exist.");
        }
        return tutorial;
    }

    private void requireCategory(Long categoryId) {
        if (categoryId == null || categoryMapper.selectById(categoryId) == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TUTORIAL_CATEGORY_NOT_FOUND",
                    "Tutorial category not found", "The referenced category does not exist.");
        }
    }

    private void assertSlugFree(String slug, Long excludeId) {
        LambdaQueryWrapper<Tutorial> wrapper =
                new LambdaQueryWrapper<Tutorial>().eq(Tutorial::getSlug, slug);
        if (excludeId != null) {
            wrapper.ne(Tutorial::getId, excludeId);
        }
        Long count = tutorialMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SLUG_CONFLICT",
                    "Slug already exists", "A tutorial with this slug already exists.");
        }
    }

    private Set<Long> resolveCategoryFilter(String categorySlug) {
        if (categorySlug == null || categorySlug.isBlank()) {
            return categoryMapper.selectList(null).stream()
                    .map(TutorialCategory::getId)
                    .collect(Collectors.toSet());
        }
        TutorialCategory category = categoryMapper.selectOne(
                new LambdaQueryWrapper<TutorialCategory>().eq(TutorialCategory::getSlug, categorySlug));
        if (category == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND",
                    "Category not found", "The category slug does not exist.");
        }
        return subtreeIds(category.getId());
    }

    private Set<Long> subtreeIds(Long rootId) {
        Map<Long, List<Long>> children = new HashMap<>();
        for (TutorialCategory category : categoryMapper.selectList(null)) {
            if (category.getParentId() != null) {
                children.computeIfAbsent(category.getParentId(), k -> new ArrayList<>()).add(category.getId());
            }
        }
        Set<Long> result = new HashSet<>();
        collect(rootId, children, result);
        return result;
    }

    private void collect(Long id, Map<Long, List<Long>> children, Set<Long> result) {
        if (!result.add(id)) {
            return;
        }
        for (Long child : children.getOrDefault(id, List.of())) {
            collect(child, children, result);
        }
    }

    private List<CategoryPathView> categoryPath(Long categoryId) {
        Map<Long, TutorialCategory> byId = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(TutorialCategory::getId, Function.identity()));
        List<CategoryPathView> path = new ArrayList<>();
        Long cursor = categoryId;
        while (cursor != null) {
            TutorialCategory category = byId.get(cursor);
            if (category == null) {
                break;
            }
            path.add(0, new CategoryPathView(category.getId(), category.getName(), category.getSlug()));
            cursor = category.getParentId();
        }
        return path;
    }

    private Map<Long, String> categoryNameMap() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(TutorialCategory::getId, TutorialCategory::getName));
    }

    private Map<Long, Long> publishedChapterCounts() {
        List<TutorialNode> chapters = nodeMapper.selectList(new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getNodeType, "CHAPTER")
                .eq(TutorialNode::getPublishStatus, PUBLISHED));
        return chapters.stream().collect(Collectors.groupingBy(TutorialNode::getTutorialId, Collectors.counting()));
    }

    private Map<Long, String> coverUrlMap(List<Long> mediaIds) {
        List<Long> distinct = mediaIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return mediaAssetMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(MediaAsset::getId, MediaAsset::getPublicUrl));
    }

    private String coverUrl(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        return media == null ? null : media.getPublicUrl();
    }

    private AdminTutorialDetailView toAdminDetail(Tutorial tutorial) {
        Map<Long, String> names = categoryNameMap();
        return new AdminTutorialDetailView(
                tutorial.getId(), tutorial.getCategoryId(), names.get(tutorial.getCategoryId()),
                tutorial.getTitle(), tutorial.getSlug(), tutorial.getSummary(),
                tutorial.getCoverMediaId(), tutorial.getPublishStatus(), tutorial.getSortOrder(),
                tutorial.getSeoTitle(), tutorial.getSeoDescription(),
                formatUtc(tutorial.getPublishedAt()), formatUtc(tutorial.getCreatedAt()),
                formatUtc(tutorial.getUpdatedAt()));
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
