package com.starrainnotes.portfolio.assembler;

import com.starrainnotes.portfolio.dto.AdminProjectDetailView;
import com.starrainnotes.portfolio.dto.AdminProjectPageView;
import com.starrainnotes.portfolio.dto.AdminProjectSummaryView;
import com.starrainnotes.portfolio.dto.PrevNextProjectView;
import com.starrainnotes.portfolio.dto.ProjectMediaView;
import com.starrainnotes.portfolio.dto.ProjectPrototypeView;
import com.starrainnotes.portfolio.dto.PublicProjectDetailView;
import com.starrainnotes.portfolio.dto.PublicProjectSummaryView;
import com.starrainnotes.portfolio.entity.PortfolioProject;
import com.starrainnotes.portfolio.entity.PortfolioProjectMedia;

import java.util.List;

/** Maps already-loaded portfolio rows into admin and public views. */
public final class PortfolioViewAssembler {
    private PortfolioViewAssembler() {
    }

    public static AdminProjectPageView adminPage(List<AdminProjectSummaryView> items, int page, int pageSize,
                                                 long total, int totalPages) {
        return new AdminProjectPageView(items, page, pageSize, total, totalPages);
    }

    public static AdminProjectSummaryView adminSummary(PortfolioProject project, String coverUrl,
                                                       String publishedAt, String updatedAt) {
        return new AdminProjectSummaryView(
                project.getId(), project.getTitle(), project.getSlug(), project.getSummary(), project.getRole(),
                project.getTechStack() == null ? List.of() : project.getTechStack(),
                coverUrl, project.getPublishStatus(), project.getProjectStatus(),
                Boolean.TRUE.equals(project.getFeatured()), project.getSortOrder(), publishedAt, updatedAt);
    }

    public static AdminProjectDetailView adminDetail(PortfolioProject project, String coverUrl, Integer coverWidth,
                                                     Integer coverHeight, String coverSrcSet,
                                                     ProjectPrototypeView prototype, String publishedAt,
                                                     String createdAt, String updatedAt,
                                                     List<ProjectMediaView> gallery) {
        return new AdminProjectDetailView(
                project.getId(), project.getTitle(), project.getSlug(), project.getSummary(), project.getRole(),
                project.getTechStack() == null ? List.of() : project.getTechStack(),
                project.getBodyMarkdown(), project.getCoverMediaId(), coverUrl, coverWidth, coverHeight, coverSrcSet,
                project.getRepositoryUrl(), project.getDemoUrl(), prototype, project.getPublishStatus(),
                project.getProjectStatus(), Boolean.TRUE.equals(project.getFeatured()), project.getSortOrder(),
                project.getStartedAt(), project.getCompletedAt(), project.getSeoTitle(), project.getSeoDescription(),
                publishedAt, createdAt, updatedAt, gallery);
    }

    public static PublicProjectSummaryView publicSummary(PortfolioProject project, String coverUrl, String coverSrcSet,
                                                         Integer coverWidth, Integer coverHeight, String updatedAt) {
        return new PublicProjectSummaryView(
                project.getId(), project.getTitle(), project.getSlug(), project.getSummary(), project.getRole(),
                project.getTechStack() == null ? List.of() : project.getTechStack(),
                coverUrl, coverSrcSet, coverWidth, coverHeight, project.getProjectStatus(),
                Boolean.TRUE.equals(project.getFeatured()), project.getSortOrder(), updatedAt);
    }

    public static PublicProjectDetailView publicDetail(PortfolioProject project, String coverUrl, Integer coverWidth,
                                                       Integer coverHeight, String coverSrcSet, String repositoryUrl,
                                                       String demoUrl, String prototypeEntry, String publishedAt,
                                                       String updatedAt, PrevNextProjectView previous,
                                                       PrevNextProjectView next, List<ProjectMediaView> gallery) {
        return new PublicProjectDetailView(
                project.getId(), project.getTitle(), project.getSlug(), project.getSummary(), project.getRole(),
                project.getTechStack() == null ? List.of() : project.getTechStack(), project.getBodyMarkdown(),
                coverUrl, coverWidth, coverHeight, coverSrcSet, repositoryUrl, demoUrl, prototypeEntry,
                project.getProjectStatus(), project.getStartedAt(), project.getCompletedAt(),
                project.getSeoTitle(), project.getSeoDescription(), publishedAt, updatedAt, previous, next, gallery);
    }

    public static ProjectMediaView galleryItem(PortfolioProjectMedia row, String url, Integer width, Integer height,
                                               String srcSet) {
        return new ProjectMediaView(
                row.getId(), row.getMediaAssetId(), url, row.getTitle(), row.getDescription(), row.getAltText(),
                row.getDeviceType(), row.getSortOrder(), width, height, srcSet);
    }

    public static PrevNextProjectView prevNext(PortfolioProject project) {
        return new PrevNextProjectView(project.getId(), project.getSlug(), project.getTitle());
    }
}
