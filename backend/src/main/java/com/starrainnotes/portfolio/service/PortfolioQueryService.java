package com.starrainnotes.portfolio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.api.MediaAssetPort;
import com.starrainnotes.media.api.MediaAssetPort.MediaImageView;
import com.starrainnotes.portfolio.assembler.PortfolioViewAssembler;
import com.starrainnotes.portfolio.dto.AdminProjectDetailView;
import com.starrainnotes.portfolio.dto.AdminProjectPageView;
import com.starrainnotes.portfolio.dto.AdminProjectSummaryView;
import com.starrainnotes.portfolio.dto.PrevNextProjectView;
import com.starrainnotes.portfolio.dto.ProjectMediaView;
import com.starrainnotes.portfolio.dto.PublicProjectDetailView;
import com.starrainnotes.portfolio.dto.PublicProjectSummaryView;
import com.starrainnotes.portfolio.entity.PortfolioProject;
import com.starrainnotes.portfolio.entity.PortfolioProjectMedia;
import com.starrainnotes.portfolio.mapper.PortfolioProjectMapper;
import com.starrainnotes.portfolio.mapper.PortfolioProjectMediaMapper;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Read-side portfolio projections for admin pages, public pages and command responses. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioQueryService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String PUBLISHED = "PUBLISHED";

    private final PortfolioProjectMapper projectMapper;
    private final PortfolioProjectMediaMapper mediaMapper;
    private final MediaAssetPort media;
    private final PortfolioPrototypeService prototypeService;
    private final SiteSettingsTimezone timezone;

    public AdminProjectPageView adminList(int page, int pageSize, String status, String projectStatus, String query) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        LambdaQueryWrapper<PortfolioProject> wrapper = new LambdaQueryWrapper<PortfolioProject>()
                .eq(status != null && !status.isBlank(), PortfolioProject::getPublishStatus, status)
                .eq(projectStatus != null && !projectStatus.isBlank(), PortfolioProject::getProjectStatus, projectStatus)
                .and(query != null && !query.isBlank(), candidate -> candidate
                        .like(PortfolioProject::getTitle, query).or().like(PortfolioProject::getSlug, query))
                .orderByDesc(PortfolioProject::getUpdatedAt)
                .orderByDesc(PortfolioProject::getId);
        Page<PortfolioProject> result = projectMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        Map<Long, String> covers = coverUrls(result.getRecords().stream().map(PortfolioProject::getCoverMediaId).toList());
        List<AdminProjectSummaryView> items = result.getRecords().stream()
                .map(project -> PortfolioViewAssembler.adminSummary(
                        project,
                        project.getCoverMediaId() == null ? null : covers.get(project.getCoverMediaId()),
                        formatUtc(project.getPublishedAt()), formatUtc(project.getUpdatedAt())))
                .toList();
        long total = result.getTotal();
        int totalPages = total == 0 ? 0 : (int) ((total + safeSize - 1) / safeSize);
        return PortfolioViewAssembler.adminPage(items, safePage, safeSize, total, totalPages);
    }

    public AdminProjectDetailView adminDetail(Long projectId) {
        PortfolioProject project = requireProject(projectId);
        MediaLink cover = mediaLink(project.getCoverMediaId());
        return PortfolioViewAssembler.adminDetail(
                project,
                cover == null ? null : cover.url(), cover == null ? null : cover.width(),
                cover == null ? null : cover.height(), cover == null ? null : cover.srcSet(),
                prototypeService.view(project.getId(), PUBLISHED.equals(project.getPublishStatus())),
                formatUtc(project.getPublishedAt()), formatUtc(project.getCreatedAt()), formatUtc(project.getUpdatedAt()),
                listGallery(project.getId()));
    }

    public List<PublicProjectSummaryView> publicList() {
        List<PortfolioProject> rows = publishedProjects();
        Map<Long, MediaLink> covers = mediaLinks(rows.stream().map(PortfolioProject::getCoverMediaId).toList());
        return rows.stream().map(project -> {
            MediaLink cover = project.getCoverMediaId() == null ? null : covers.get(project.getCoverMediaId());
            return PortfolioViewAssembler.publicSummary(
                    project,
                    cover == null ? null : cover.url(), cover == null ? null : cover.srcSet(),
                    cover == null ? null : cover.width(), cover == null ? null : cover.height(),
                    formatUtc(project.getUpdatedAt()));
        }).toList();
    }

    public PublicProjectDetailView publicDetail(String slug) {
        PortfolioProject project = projectMapper.selectOne(
                new LambdaQueryWrapper<PortfolioProject>().eq(PortfolioProject::getSlug, slug));
        if (project == null || !PUBLISHED.equals(project.getPublishStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND",
                    "Project not found", "The project does not exist or is not public.");
        }
        List<PortfolioProject> ordered = publishedProjects();
        int currentIndex = java.util.stream.IntStream.range(0, ordered.size())
                .filter(index -> ordered.get(index).getId().equals(project.getId()))
                .findFirst().orElse(-1);
        PrevNextProjectView previous = currentIndex > 0 ? PortfolioViewAssembler.prevNext(ordered.get(currentIndex - 1)) : null;
        PrevNextProjectView next = currentIndex >= 0 && currentIndex + 1 < ordered.size()
                ? PortfolioViewAssembler.prevNext(ordered.get(currentIndex + 1)) : null;
        MediaLink cover = mediaLink(project.getCoverMediaId());
        return PortfolioViewAssembler.publicDetail(
                project,
                cover == null ? null : cover.url(), cover == null ? null : cover.width(),
                cover == null ? null : cover.height(), cover == null ? null : cover.srcSet(),
                normalizeHttpUrl(project.getRepositoryUrl()), usableDemoUrl(project.getDemoUrl(), project.getRepositoryUrl()),
                prototypeService.publicEntry(project.getId(), true),
                formatUtc(project.getPublishedAt()), formatUtc(project.getUpdatedAt()), previous, next, listGallery(project.getId()));
    }

    private PortfolioProject requireProject(Long projectId) {
        PortfolioProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND",
                    "Project not found", "The portfolio project does not exist.");
        }
        return project;
    }

    private List<PortfolioProject> publishedProjects() {
        return projectMapper.selectList(new LambdaQueryWrapper<PortfolioProject>()
                .eq(PortfolioProject::getPublishStatus, PUBLISHED)
                .orderByDesc(PortfolioProject::getFeatured)
                .orderByAsc(PortfolioProject::getSortOrder)
                .orderByAsc(PortfolioProject::getId));
    }

    private MediaLink mediaLink(Long mediaId) {
        MediaImageView image = media.image(mediaId);
        return image == null ? null : toMediaLink(image);
    }

    private Map<Long, String> coverUrls(List<Long> mediaIds) {
        return mediaLinks(mediaIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().url()));
    }

    private Map<Long, MediaLink> mediaLinks(List<Long> mediaIds) {
        return media.images(mediaIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> toMediaLink(entry.getValue())));
    }

    private MediaLink toMediaLink(MediaImageView image) {
        return new MediaLink(image.url(), image.width(), image.height(), image.srcSet());
    }

    private List<ProjectMediaView> listGallery(Long projectId) {
        List<PortfolioProjectMedia> rows = mediaMapper.selectList(new LambdaQueryWrapper<PortfolioProjectMedia>()
                .eq(PortfolioProjectMedia::getProjectId, projectId)
                .orderByAsc(PortfolioProjectMedia::getSortOrder)
                .orderByAsc(PortfolioProjectMedia::getId));
        if (rows.isEmpty()) return List.of();
        Map<Long, MediaLink> links = mediaLinks(rows.stream().map(PortfolioProjectMedia::getMediaAssetId).toList());
        return rows.stream().map(row -> {
            MediaLink link = links.get(row.getMediaAssetId());
            return PortfolioViewAssembler.galleryItem(
                    row, link == null ? null : link.url(),
                    link == null ? null : link.width(), link == null ? null : link.height(),
                    link == null ? null : link.srcSet());
        }).toList();
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
                    || !StringUtils.hasText(uri.getHost())) throw invalidExternalUrl();
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

    private String formatUtc(LocalDateTime utc) {
        return utc == null ? null : timezone.atSite(utc).format(ISO_OFFSET);
    }

    private record MediaLink(String url, Integer width, Integer height, String srcSet) {
    }
}
