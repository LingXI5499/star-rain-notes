package com.starrainnotes.portfolio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.portfolio.dto.AdminProjectDetailView;
import com.starrainnotes.portfolio.dto.AdminProjectPageView;
import com.starrainnotes.portfolio.dto.AdminProjectSummaryView;
import com.starrainnotes.portfolio.dto.CreateProjectRequest;
import com.starrainnotes.portfolio.dto.PublicProjectDetailView;
import com.starrainnotes.portfolio.dto.PublicProjectSummaryView;
import com.starrainnotes.portfolio.dto.PrevNextProjectView;
import com.starrainnotes.portfolio.dto.UpdateProjectRequest;
import com.starrainnotes.portfolio.entity.PortfolioProject;
import com.starrainnotes.portfolio.mapper.PortfolioProjectMapper;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Portfolio vertical slice (01 §4, 04 §12).
 *
 * <p>publish_status and project_status are fully independent; PUBLISHED +
 * DEVELOPING is legal. Service-enforced rules: ONLINE requires demoUrl,
 * featured ≤ 3, completedAt ≥ startedAt, techStack 0-20 items normalized
 * (trim / drop blanks / dedupe / preserve order), cover must be IMAGE.</p>
 */
@Service
public class PortfolioService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int MAX_FEATURED = 3;
    private static final int MAX_TECH_STACK = 20;
    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";
    private static final String WITHDRAWN = "WITHDRAWN";
    private static final String ONLINE = "ONLINE";
    private static final String DEVELOPING = "DEVELOPING";

    private final PortfolioProjectMapper projectMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final SiteSettingsTimezone timezone;

    public PortfolioService(PortfolioProjectMapper projectMapper,
                            MediaAssetMapper mediaAssetMapper,
                            SiteSettingsTimezone timezone) {
        this.projectMapper = projectMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.timezone = timezone;
    }

    // ---------------------------------------------------------------
    // admin
    // ---------------------------------------------------------------

    public AdminProjectPageView adminList(int page, int pageSize, String status, String projectStatus, String query) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        LambdaQueryWrapper<PortfolioProject> wrapper = new LambdaQueryWrapper<PortfolioProject>()
                .eq(status != null && !status.isBlank(), PortfolioProject::getPublishStatus, status)
                .eq(projectStatus != null && !projectStatus.isBlank(), PortfolioProject::getProjectStatus, projectStatus)
                .and(query != null && !query.isBlank(), w -> w
                        .like(PortfolioProject::getTitle, query).or().like(PortfolioProject::getSlug, query))
                .orderByDesc(PortfolioProject::getUpdatedAt)
                .orderByDesc(PortfolioProject::getId);

        Long total = projectMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + safeSize + " OFFSET " + ((safePage - 1) * safeSize));
        List<PortfolioProject> rows = projectMapper.selectList(wrapper);
        Map<Long, String> covers = coverUrls(rows.stream().map(PortfolioProject::getCoverMediaId).toList());
        List<AdminProjectSummaryView> items = rows.stream()
                .map(p -> new AdminProjectSummaryView(
                        p.getId(), p.getTitle(), p.getSlug(), p.getSummary(), p.getRole(),
                        p.getTechStack() == null ? List.of() : p.getTechStack(),
                        p.getCoverMediaId() == null ? null : covers.get(p.getCoverMediaId()),
                        p.getPublishStatus(), p.getProjectStatus(),
                        Boolean.TRUE.equals(p.getFeatured()), p.getSortOrder(),
                        formatUtc(p.getPublishedAt()), formatUtc(p.getUpdatedAt())))
                .toList();

        long safeTotal = total == null ? 0 : total;
        int totalPages = safeTotal == 0 ? 0 : (int) ((safeTotal + safeSize - 1) / safeSize);
        return new AdminProjectPageView(items, safePage, safeSize, safeTotal, totalPages);
    }

    public AdminProjectDetailView adminDetail(Long projectId) {
        return toAdminDetail(requireProject(projectId));
    }

    public AdminProjectDetailView create(CreateProjectRequest request) {
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> slugExists(candidate, null));
        assertSlugFree(slug, null);
        validateRules(request.demoUrl(), request.projectStatus(), request.featured(), null,
                request.startedAt(), request.completedAt(), request.coverMediaId());
        List<String> techStack = normalizeTechStack(request.techStack());

        PortfolioProject project = new PortfolioProject();
        applyFields(project, request.title(), slug, request.summary(), request.role(), techStack,
                request.bodyMarkdown(), request.coverMediaId(), request.repositoryUrl(), request.demoUrl(),
                request.projectStatus(), request.featured(), request.sortOrder(),
                request.startedAt(), request.completedAt());
        if (project.getSortOrder() == null) project.setSortOrder(nextProjectOrder());
        project.setPublishStatus(DRAFT);
        project.setPublishedAt(null);
        projectMapper.insert(project);
        return toAdminDetail(project);
    }

    public AdminProjectDetailView update(Long projectId, UpdateProjectRequest request) {
        PortfolioProject project = requireProject(projectId);
        String slug = NumericSlugGenerator.forUpdate(request.slug(), project.getSlug());
        assertSlugFree(slug, projectId);
        validateRules(request.demoUrl(), request.projectStatus(), request.featured(), projectId,
                request.startedAt(), request.completedAt(), request.coverMediaId());
        List<String> techStack = normalizeTechStack(request.techStack());

        applyFields(project, request.title(), slug, request.summary(), request.role(), techStack,
                request.bodyMarkdown(), request.coverMediaId(), request.repositoryUrl(), request.demoUrl(),
                request.projectStatus(), request.featured(), request.sortOrder(),
                request.startedAt(), request.completedAt());
        // publishStatus / publishedAt are never touched by a plain update
        projectMapper.updateById(project);
        return toAdminDetail(project);
    }

    public void delete(Long projectId) {
        requireProject(projectId);
        projectMapper.deleteById(projectId);
    }

    public AdminProjectDetailView publish(Long projectId) {
        PortfolioProject project = requireProject(projectId);
        if (!PUBLISHED.equals(project.getPublishStatus())) {
            if (project.getPublishedAt() == null) {
                project.setPublishedAt(LocalDateTime.now(Clock.systemUTC()));
            }
            project.setPublishStatus(PUBLISHED);
            projectMapper.updateById(project);
        }
        return toAdminDetail(project);
    }

    public AdminProjectDetailView withdraw(Long projectId) {
        PortfolioProject project = requireProject(projectId);
        if (DRAFT.equals(project.getPublishStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PUBLISH_TRANSITION",
                    "Cannot withdraw a draft", "Only published projects can be withdrawn.");
        }
        if (!WITHDRAWN.equals(project.getPublishStatus())) {
            project.setPublishStatus(WITHDRAWN);
            projectMapper.updateById(project);
        }
        return toAdminDetail(project);
    }

    // ---------------------------------------------------------------
    // public (no pagination; featured first)
    // ---------------------------------------------------------------

    public List<PublicProjectSummaryView> publicList() {
        List<PortfolioProject> rows = projectMapper.selectList(new LambdaQueryWrapper<PortfolioProject>()
                .eq(PortfolioProject::getPublishStatus, PUBLISHED)
                .orderByDesc(PortfolioProject::getFeatured)
                .orderByAsc(PortfolioProject::getSortOrder)
                .orderByAsc(PortfolioProject::getId));
        Map<Long, String> covers = coverUrls(rows.stream().map(PortfolioProject::getCoverMediaId).toList());
        return rows.stream()
                .map(p -> new PublicProjectSummaryView(
                        p.getId(), p.getTitle(), p.getSlug(), p.getSummary(), p.getRole(),
                        p.getTechStack() == null ? List.of() : p.getTechStack(),
                        p.getCoverMediaId() == null ? null : covers.get(p.getCoverMediaId()), p.getProjectStatus(),
                        Boolean.TRUE.equals(p.getFeatured()), p.getSortOrder(),
                        formatUtc(p.getUpdatedAt())))
                .toList();
    }

    public PublicProjectDetailView publicDetail(String slug) {
        PortfolioProject project = projectMapper.selectOne(
                new LambdaQueryWrapper<PortfolioProject>().eq(PortfolioProject::getSlug, slug));
        if (project == null || !PUBLISHED.equals(project.getPublishStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND",
                    "Project not found", "The project does not exist or is not public.");
        }
        List<String> techStack = project.getTechStack() == null ? List.of() : project.getTechStack();
        List<PortfolioProject> ordered = projectMapper.selectList(new LambdaQueryWrapper<PortfolioProject>()
                .eq(PortfolioProject::getPublishStatus, PUBLISHED)
                .orderByDesc(PortfolioProject::getFeatured)
                .orderByAsc(PortfolioProject::getSortOrder)
                .orderByAsc(PortfolioProject::getId));
        int currentIndex = java.util.stream.IntStream.range(0, ordered.size())
                .filter(index -> ordered.get(index).getId().equals(project.getId()))
                .findFirst().orElse(-1);
        PrevNextProjectView previous = currentIndex > 0 ? toPrevNext(ordered.get(currentIndex - 1)) : null;
        PrevNextProjectView next = currentIndex >= 0 && currentIndex + 1 < ordered.size()
                ? toPrevNext(ordered.get(currentIndex + 1)) : null;
        return new PublicProjectDetailView(
                project.getId(), project.getTitle(), project.getSlug(), project.getSummary(), project.getRole(),
                techStack, project.getBodyMarkdown(), coverUrl(project.getCoverMediaId()),
                project.getRepositoryUrl(), project.getDemoUrl(), project.getProjectStatus(),
                project.getStartedAt(), project.getCompletedAt(),
                project.getSeoTitle(), project.getSeoDescription(),
                formatUtc(project.getPublishedAt()), formatUtc(project.getUpdatedAt()), previous, next);
    }

    // ---------------------------------------------------------------
    // rules
    // ---------------------------------------------------------------

    private void validateRules(String demoUrl, String projectStatus, Boolean featured, Long excludeId,
                               java.time.LocalDate startedAt, java.time.LocalDate completedAt, Long coverMediaId) {
        String effectiveStatus = projectStatus == null ? DEVELOPING : projectStatus;
        if (ONLINE.equals(effectiveStatus) && !StringUtils.hasText(demoUrl)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DEMO_URL_REQUIRED",
                    "Demo URL required", "An ONLINE project must provide a demoUrl.");
        }
        if (startedAt != null && completedAt != null && completedAt.isBefore(startedAt)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_DATE_RANGE",
                    "Invalid date range", "completedAt must not be before startedAt.");
        }
        if (Boolean.TRUE.equals(featured)) {
            Long featuredCount = projectMapper.selectCount(new LambdaQueryWrapper<PortfolioProject>()
                    .eq(PortfolioProject::getFeatured, true)
                    .ne(excludeId != null, PortfolioProject::getId, excludeId));
            if (featuredCount != null && featuredCount >= MAX_FEATURED) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "FEATURED_LIMIT_EXCEEDED",
                        "Featured limit exceeded", "At most " + MAX_FEATURED + " projects can be featured.");
            }
        }
        validateImageMedia(coverMediaId);
    }

    private List<String> normalizeTechStack(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>(new LinkedHashSet<>());
        for (String item : raw) {
            if (item == null) {
                continue;
            }
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        if (normalized.size() > MAX_TECH_STACK) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TECH_STACK_TOO_LARGE",
                    "Tech stack too large", "At most " + MAX_TECH_STACK + " tech stack items are allowed.");
        }
        return normalized;
    }

    private void validateImageMedia(Long mediaId) {
        if (mediaId == null) {
            return;
        }
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        if (media == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_NOT_FOUND",
                    "Media not found", "The referenced cover media asset does not exist.");
        }
        if (!"IMAGE".equals(media.getAssetType())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_TYPE_INVALID",
                    "Only IMAGE media allowed", "The cover must reference an IMAGE asset.");
        }
    }

    private void applyFields(PortfolioProject p, String title, String slug, String summary, String role,
                             List<String> techStack, String bodyMarkdown, Long coverMediaId,
                             String repositoryUrl, String demoUrl, String projectStatus, Boolean featured,
                             Integer sortOrder, java.time.LocalDate startedAt,
                             java.time.LocalDate completedAt) {
        p.setTitle(title);
        p.setSlug(slug);
        p.setSummary(summary);
        p.setRole(role);
        p.setTechStack(techStack);
        p.setBodyMarkdown(bodyMarkdown);
        p.setCoverMediaId(coverMediaId);
        p.setRepositoryUrl(repositoryUrl);
        p.setDemoUrl(demoUrl);
        p.setProjectStatus(projectStatus == null ? DEVELOPING : projectStatus);
        p.setFeatured(Boolean.TRUE.equals(featured));
        if (sortOrder != null) p.setSortOrder(sortOrder);
        p.setStartedAt(startedAt);
        p.setCompletedAt(completedAt);
    }

    private PortfolioProject requireProject(Long projectId) {
        PortfolioProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND",
                    "Project not found", "The portfolio project does not exist.");
        }
        return project;
    }

    private void assertSlugFree(String slug, Long excludeId) {
        LambdaQueryWrapper<PortfolioProject> wrapper =
                new LambdaQueryWrapper<PortfolioProject>().eq(PortfolioProject::getSlug, slug);
        if (excludeId != null) {
            wrapper.ne(PortfolioProject::getId, excludeId);
        }
        if (projectMapper.selectCount(wrapper) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SLUG_CONFLICT",
                    "Slug already exists", "A project with this slug already exists.");
        }
    }

    private boolean slugExists(String slug, Long excludeId) {
        LambdaQueryWrapper<PortfolioProject> wrapper =
                new LambdaQueryWrapper<PortfolioProject>().eq(PortfolioProject::getSlug, slug);
        if (excludeId != null) wrapper.ne(PortfolioProject::getId, excludeId);
        Long count = projectMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private int nextProjectOrder() {
        return projectMapper.selectList(null).stream()
                .mapToInt(project -> project.getSortOrder() == null ? 0 : project.getSortOrder())
                .max().orElse(0) + 10;
    }

    private String coverUrl(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        return media == null ? null : media.getPublicUrl();
    }

    private Map<Long, String> coverUrls(List<Long> mediaIds) {
        List<Long> distinct = mediaIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return mediaAssetMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(MediaAsset::getId, MediaAsset::getPublicUrl));
    }

    private AdminProjectDetailView toAdminDetail(PortfolioProject p) {
        List<String> techStack = p.getTechStack() == null ? List.of() : p.getTechStack();
        return new AdminProjectDetailView(
                p.getId(), p.getTitle(), p.getSlug(), p.getSummary(), p.getRole(), techStack,
                p.getBodyMarkdown(), p.getCoverMediaId(), coverUrl(p.getCoverMediaId()),
                p.getRepositoryUrl(), p.getDemoUrl(),
                p.getPublishStatus(), p.getProjectStatus(), Boolean.TRUE.equals(p.getFeatured()),
                p.getSortOrder(), p.getStartedAt(), p.getCompletedAt(), p.getSeoTitle(), p.getSeoDescription(),
                formatUtc(p.getPublishedAt()), formatUtc(p.getCreatedAt()), formatUtc(p.getUpdatedAt()));
    }

    private PrevNextProjectView toPrevNext(PortfolioProject project) {
        return new PrevNextProjectView(project.getId(), project.getSlug(), project.getTitle());
    }

    private String formatUtc(LocalDateTime utc) {
        if (utc == null) {
            return null;
        }
        return timezone.atSite(utc).format(ISO_OFFSET);
    }
}
