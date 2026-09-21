package com.starrainnotes.portfolio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.service.MediaService;
import com.starrainnotes.portfolio.dto.AdminProjectDetailView;
import com.starrainnotes.portfolio.dto.AdminProjectPageView;
import com.starrainnotes.portfolio.dto.AdminProjectSummaryView;
import com.starrainnotes.portfolio.dto.CreateProjectRequest;
import com.starrainnotes.portfolio.dto.ProjectMediaItemRequest;
import com.starrainnotes.portfolio.dto.ProjectMediaView;
import com.starrainnotes.portfolio.dto.PublicProjectDetailView;
import com.starrainnotes.portfolio.dto.PublicProjectSummaryView;
import com.starrainnotes.portfolio.dto.PrevNextProjectView;
import com.starrainnotes.portfolio.dto.UpdateProjectRequest;
import com.starrainnotes.portfolio.entity.PortfolioProject;
import com.starrainnotes.portfolio.entity.PortfolioProjectMedia;
import com.starrainnotes.portfolio.mapper.PortfolioProjectMapper;
import com.starrainnotes.portfolio.mapper.PortfolioProjectMediaMapper;
import com.starrainnotes.seo.SeoContentChange;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Portfolio vertical slice (01 §4, 04 §12).
 *
 * <p>publish_status and project_status are fully independent; PUBLISHED +
 * DEVELOPING is legal. Service-enforced rules: ONLINE requires demoUrl
 * (aligned with ck_portfolio_online_demo), featured ≤ 3, completedAt ≥ startedAt,
 * techStack 0-20 items normalized (trim / drop blanks / dedupe / preserve order),
 * cover must be IMAGE; publishing also requires a cover image.</p>
 */
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int MAX_FEATURED = 3;
    private static final int MAX_TECH_STACK = 20;
    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";
    private static final String WITHDRAWN = "WITHDRAWN";
    private static final String ONLINE = "ONLINE";
    private static final String DEVELOPING = "DEVELOPING";

    private static final int MAX_GALLERY = 24;

    private final PortfolioProjectMapper projectMapper;
    private final PortfolioProjectMediaMapper mediaMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final MediaService mediaService;
    private final PortfolioPrototypeService prototypeService;
    private final SiteSettingsTimezone timezone;

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

        Page<PortfolioProject> result = projectMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<PortfolioProject> rows = result.getRecords();
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

        long safeTotal = result.getTotal();
        int totalPages = safeTotal == 0 ? 0 : (int) ((safeTotal + safeSize - 1) / safeSize);
        return new AdminProjectPageView(items, safePage, safeSize, safeTotal, totalPages);
    }

    public AdminProjectDetailView adminDetail(Long projectId) {
        return toAdminDetail(requireProject(projectId));
    }

    @Transactional
    public AdminProjectDetailView create(CreateProjectRequest request) {
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> slugExists(candidate, null));
        assertSlugFree(slug, null);
        validateRules(request.repositoryUrl(), request.demoUrl(), request.projectStatus(), request.featured(), null,
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
        replaceGallery(project.getId(), request.gallery());
        return toAdminDetail(project);
    }

    @Transactional
    @SeoContentChange(table = "portfolio_project", pathPrefix = "/portfolio/")
    public AdminProjectDetailView update(Long projectId, UpdateProjectRequest request) {
        PortfolioProject project = requireProject(projectId);
        String slug = NumericSlugGenerator.forUpdate(request.slug(), project.getSlug());
        assertSlugFree(slug, projectId);
        validateRules(request.repositoryUrl(), request.demoUrl(), request.projectStatus(), request.featured(), projectId,
                request.startedAt(), request.completedAt(), request.coverMediaId());
        List<String> techStack = normalizeTechStack(request.techStack());

        applyFields(project, request.title(), slug, request.summary(), request.role(), techStack,
                request.bodyMarkdown(), request.coverMediaId(), request.repositoryUrl(), request.demoUrl(),
                request.projectStatus(), request.featured(), request.sortOrder(),
                request.startedAt(), request.completedAt());
        // publishStatus / publishedAt are never touched by a plain update
        projectMapper.updateById(project);
        replaceGallery(projectId, request.gallery());
        return toAdminDetail(project);
    }

    @SeoContentChange(table = "portfolio_project", pathPrefix = "/portfolio/")
    public void delete(Long projectId) {
        requireProject(projectId);
        prototypeService.delete(projectId);
        projectMapper.deleteById(projectId);
    }

    @SeoContentChange(table = "portfolio_project", pathPrefix = "/portfolio/")
    public AdminProjectDetailView publish(Long projectId) {
        PortfolioProject project = requireProject(projectId);
        validatePublishable(project);
        if (!PUBLISHED.equals(project.getPublishStatus())) {
            if (project.getPublishedAt() == null) {
                project.setPublishedAt(LocalDateTime.now(Clock.systemUTC()));
            }
            project.setPublishStatus(PUBLISHED);
            projectMapper.updateById(project);
        }
        prototypeService.publish(projectId);
        return toAdminDetail(project);
    }

    @SeoContentChange(table = "portfolio_project", pathPrefix = "/portfolio/")
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
        Map<Long, MediaLink> covers = mediaLinks(rows.stream().map(PortfolioProject::getCoverMediaId).toList());
        return rows.stream()
                .map(p -> {
                    MediaLink cover = p.getCoverMediaId() == null ? null : covers.get(p.getCoverMediaId());
                    return new PublicProjectSummaryView(
                            p.getId(), p.getTitle(), p.getSlug(), p.getSummary(), p.getRole(),
                            p.getTechStack() == null ? List.of() : p.getTechStack(),
                            cover == null ? null : cover.url(),
                            cover == null ? null : cover.srcSet(),
                            cover == null ? null : cover.width(),
                            cover == null ? null : cover.height(),
                            p.getProjectStatus(),
                            Boolean.TRUE.equals(p.getFeatured()), p.getSortOrder(),
                            formatUtc(p.getUpdatedAt()));
                })
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
        MediaLink cover = mediaLink(project.getCoverMediaId());
        return new PublicProjectDetailView(
                project.getId(), project.getTitle(), project.getSlug(), project.getSummary(), project.getRole(),
                techStack, project.getBodyMarkdown(),
                cover == null ? null : cover.url(),
                cover == null ? null : cover.width(),
                cover == null ? null : cover.height(),
                cover == null ? null : cover.srcSet(),
                normalizeHttpUrl(project.getRepositoryUrl()), usableDemoUrl(project.getDemoUrl(), project.getRepositoryUrl()),
                prototypeService.publicEntry(project.getId(), true), project.getProjectStatus(),
                project.getStartedAt(), project.getCompletedAt(),
                project.getSeoTitle(), project.getSeoDescription(),
                formatUtc(project.getPublishedAt()), formatUtc(project.getUpdatedAt()), previous, next,
                listGallery(project.getId()));
    }

    // ---------------------------------------------------------------
    // rules
    // ---------------------------------------------------------------

    private void validateRules(String repositoryUrl, String demoUrl, String projectStatus, Boolean featured, Long excludeId,
                               java.time.LocalDate startedAt, java.time.LocalDate completedAt, Long coverMediaId) {
        normalizeHttpUrl(repositoryUrl);
        String normalizedDemoUrl = usableDemoUrl(demoUrl, repositoryUrl);
        if (ONLINE.equals(projectStatus) && !StringUtils.hasText(normalizedDemoUrl)
                && (excludeId == null || !prototypeService.exists(excludeId))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DEMO_URL_REQUIRED",
                    "Online access required", "An ONLINE project requires a live URL or an uploaded static prototype.");
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

    private void validatePublishable(PortfolioProject project) {
        if (project.getCoverMediaId() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "COVER_REQUIRED", "Cover required", "Published projects must have a theme cover image.");
        }
        validateImageMedia(project.getCoverMediaId());
        if (ONLINE.equals(project.getProjectStatus())
                && !StringUtils.hasText(project.getDemoUrl())
                && !prototypeService.exists(project.getId())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DEMO_REQUIRED", "Online access required",
                    "An ONLINE project needs a live URL or an uploaded static prototype.");
        }
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
        p.setRepositoryUrl(normalizeHttpUrl(repositoryUrl));
        p.setDemoUrl(usableDemoUrl(demoUrl, repositoryUrl));
        p.setProjectStatus(projectStatus == null ? DEVELOPING : projectStatus);
        p.setFeatured(Boolean.TRUE.equals(featured));
        if (sortOrder != null) p.setSortOrder(sortOrder);
        p.setStartedAt(startedAt);
        p.setCompletedAt(completedAt);
    }

    private String usableDemoUrl(String rawDemoUrl, String rawRepositoryUrl) {
        String demoUrl = normalizeHttpUrl(rawDemoUrl);
        if (demoUrl == null) return null;
        String repositoryUrl = normalizeHttpUrl(rawRepositoryUrl);
        String host = URI.create(demoUrl).getHost().toLowerCase();
        if ("github.com".equals(host) || "www.github.com".equals(host)
                || (repositoryUrl != null && withoutTrailingSlash(demoUrl).equalsIgnoreCase(withoutTrailingSlash(repositoryUrl)))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DEMO_URL_IS_REPOSITORY",
                    "Live URL cannot be a repository", "The online URL must point to a deployed site. Put GitHub links in the repository URL field.");
        }
        return demoUrl;
    }

    private String normalizeHttpUrl(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) return null;
        String value = rawUrl.trim();
        if (!value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) value = "https://" + value;
        try {
            URI uri = new URI(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || !StringUtils.hasText(uri.getHost())) {
                throw invalidExternalUrl();
            }
            return uri.toString();
        } catch (URISyntaxException ex) {
            throw invalidExternalUrl();
        }
    }

    private static String withoutTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static ApiException invalidExternalUrl() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_EXTERNAL_URL",
                "Invalid external URL", "Repository and online URLs must be valid HTTP or HTTPS addresses.");
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
        MediaLink link = mediaLink(mediaId);
        return link == null ? null : link.url();
    }

    private MediaLink mediaLink(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        if (media == null) {
            return null;
        }
        return toMediaLink(media);
    }

    private Map<Long, String> coverUrls(List<Long> mediaIds) {
        return mediaLinks(mediaIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().url()));
    }

    private Map<Long, MediaLink> mediaLinks(List<Long> mediaIds) {
        List<Long> distinct = mediaIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return mediaAssetMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(MediaAsset::getId, this::toMediaLink));
    }

    private MediaLink toMediaLink(MediaAsset media) {
        return new MediaLink(
                media.getPublicUrl(),
                media.getWidth(),
                media.getHeight(),
                mediaService.srcSetOf(media));
    }

    private AdminProjectDetailView toAdminDetail(PortfolioProject p) {
        List<String> techStack = p.getTechStack() == null ? List.of() : p.getTechStack();
        MediaLink cover = mediaLink(p.getCoverMediaId());
        return new AdminProjectDetailView(
                p.getId(), p.getTitle(), p.getSlug(), p.getSummary(), p.getRole(), techStack,
                p.getBodyMarkdown(), p.getCoverMediaId(),
                cover == null ? null : cover.url(),
                cover == null ? null : cover.width(),
                cover == null ? null : cover.height(),
                cover == null ? null : cover.srcSet(),
                p.getRepositoryUrl(), p.getDemoUrl(), prototypeService.view(p.getId(), PUBLISHED.equals(p.getPublishStatus())),
                p.getPublishStatus(), p.getProjectStatus(), Boolean.TRUE.equals(p.getFeatured()),
                p.getSortOrder(), p.getStartedAt(), p.getCompletedAt(), p.getSeoTitle(), p.getSeoDescription(),
                formatUtc(p.getPublishedAt()), formatUtc(p.getCreatedAt()), formatUtc(p.getUpdatedAt()),
                listGallery(p.getId()));
    }

    private List<ProjectMediaView> listGallery(Long projectId) {
        List<PortfolioProjectMedia> rows = mediaMapper.selectList(new LambdaQueryWrapper<PortfolioProjectMedia>()
                .eq(PortfolioProjectMedia::getProjectId, projectId)
                .orderByAsc(PortfolioProjectMedia::getSortOrder)
                .orderByAsc(PortfolioProjectMedia::getId));
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, MediaLink> links = mediaLinks(rows.stream().map(PortfolioProjectMedia::getMediaAssetId).toList());
        return rows.stream()
                .map(row -> {
                    MediaLink link = links.get(row.getMediaAssetId());
                    return new ProjectMediaView(
                            row.getId(),
                            row.getMediaAssetId(),
                            link == null ? null : link.url(),
                            row.getTitle(),
                            row.getDescription(),
                            row.getAltText(),
                            row.getDeviceType(),
                            row.getSortOrder(),
                            link == null ? null : link.width(),
                            link == null ? null : link.height(),
                            link == null ? null : link.srcSet());
                })
                .toList();
    }

    private record MediaLink(String url, Integer width, Integer height, String srcSet) {
    }

    private void replaceGallery(Long projectId, List<ProjectMediaItemRequest> gallery) {
        List<ProjectMediaItemRequest> items = gallery == null ? List.of() : gallery;
        if (items.size() > MAX_GALLERY) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GALLERY_TOO_LARGE",
                    "Gallery too large", "At most " + MAX_GALLERY + " gallery images are allowed.");
        }
        for (ProjectMediaItemRequest item : items) {
            validateImageMedia(item.mediaAssetId());
        }

        List<PortfolioProjectMedia> existing = mediaMapper.selectList(new LambdaQueryWrapper<PortfolioProjectMedia>()
                .eq(PortfolioProjectMedia::getProjectId, projectId));
        Map<Long, PortfolioProjectMedia> existingById = existing.stream()
                .collect(Collectors.toMap(PortfolioProjectMedia::getId, row -> row));
        Set<Long> keepIds = new HashSet<>();

        int index = 0;
        for (ProjectMediaItemRequest item : items) {
            int sort = item.sortOrder() != null ? item.sortOrder() : index * 10;
            String device = StringUtils.hasText(item.deviceType()) ? item.deviceType() : "DESKTOP";
            if (item.id() != null && existingById.containsKey(item.id())) {
                PortfolioProjectMedia row = existingById.get(item.id());
                row.setMediaAssetId(item.mediaAssetId());
                row.setTitle(blankToNull(item.title()));
                row.setDescription(blankToNull(item.description()));
                row.setAltText(blankToNull(item.altText()));
                row.setDeviceType(device);
                row.setSortOrder(sort);
                mediaMapper.updateById(row);
                keepIds.add(row.getId());
            } else {
                PortfolioProjectMedia row = new PortfolioProjectMedia();
                row.setProjectId(projectId);
                row.setMediaAssetId(item.mediaAssetId());
                row.setTitle(blankToNull(item.title()));
                row.setDescription(blankToNull(item.description()));
                row.setAltText(blankToNull(item.altText()));
                row.setDeviceType(device);
                row.setSortOrder(sort);
                mediaMapper.insert(row);
                keepIds.add(row.getId());
            }
            index++;
        }

        for (PortfolioProjectMedia row : existing) {
            if (!keepIds.contains(row.getId())) {
                mediaMapper.deleteById(row.getId());
            }
        }
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
