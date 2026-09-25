package com.starrainnotes.portfolio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.media.api.MediaAssetPort;
import com.starrainnotes.portfolio.dto.AdminProjectDetailView;
import com.starrainnotes.portfolio.dto.CreateProjectRequest;
import com.starrainnotes.portfolio.dto.ProjectMediaItemRequest;
import com.starrainnotes.portfolio.dto.UpdateProjectRequest;
import com.starrainnotes.portfolio.entity.PortfolioProject;
import com.starrainnotes.portfolio.entity.PortfolioProjectMedia;
import com.starrainnotes.portfolio.mapper.PortfolioProjectMapper;
import com.starrainnotes.portfolio.mapper.PortfolioProjectMediaMapper;
import com.starrainnotes.seo.SeoContentChange;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
public class PortfolioCommandService {

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
    private final MediaAssetPort media;
    private final PortfolioPrototypeService prototypeService;
    private final PortfolioQueryService queryService;

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
        return queryService.adminDetail(project.getId());
    }

    @Transactional
    @SeoContentChange(kind = "portfolio", pathPrefix = "/portfolio/")
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
        return queryService.adminDetail(projectId);
    }

    @Transactional
    @SeoContentChange(kind = "portfolio", pathPrefix = "/portfolio/")
    public void delete(Long projectId) {
        requireProject(projectId);
        prototypeService.delete(projectId);
        projectMapper.deleteById(projectId);
    }

    @Transactional
    @SeoContentChange(kind = "portfolio", pathPrefix = "/portfolio/")
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
        return queryService.adminDetail(projectId);
    }

    @Transactional
    @SeoContentChange(kind = "portfolio", pathPrefix = "/portfolio/")
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
        return queryService.adminDetail(projectId);
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
        media.requireImageIfPresent(mediaId);
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

}
