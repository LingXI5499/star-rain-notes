package com.starrainnotes.site.service;

import com.starrainnotes.blog.api.BlogPublishedPort;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.api.MediaAssetPort;
import com.starrainnotes.portfolio.api.PortfolioPublishedPort;
import com.starrainnotes.profile.api.ProfilePreviewPort;
import com.starrainnotes.site.assembler.SiteViewAssembler;
import com.starrainnotes.site.dto.AboutPreviewView;
import com.starrainnotes.site.dto.AdminSiteSettingsView;
import com.starrainnotes.site.dto.FeaturedProjectView;
import com.starrainnotes.site.dto.LatestUpdateView;
import com.starrainnotes.site.dto.PublicHomeView;
import com.starrainnotes.site.dto.PublicSiteView;
import com.starrainnotes.site.entity.SiteSetting;
import com.starrainnotes.site.repository.SiteSettingRepository;
import com.starrainnotes.tutorial.api.TutorialPublishedPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Site foundation service (TASK-004): public site config, public home data
 * (latest updates / featured projects / about preview), and admin site
 * settings CRUD against the frozen singleton.
 *
 * <p>Time rule: the database stores UTC; API timestamps are converted to the
 * site timezone (03 §1 / 04 §4). Public views never expose storage paths or
 * media ids.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteQueryService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int LATEST_LIMIT = 6;
    private static final int FEATURED_LIMIT = 3;

    private final SiteSettingRepository settings;
    private final SiteSettingsTimezone siteSettingsTimezone;
    private final MediaAssetPort media;
    private final TutorialPublishedPort tutorials;
    private final BlogPublishedPort blogs;
    private final PortfolioPublishedPort projects;
    private final ProfilePreviewPort profiles;

    public PublicSiteView getPublicSite() {
        SiteSetting setting = settings.findSingleton();
        if (setting == null) {
            throw new EmptyResultDataAccessException(1);
        }
        return SiteViewAssembler.publicSite(setting, media.publicUrl(setting.getLogoMediaId()),
                media.publicUrl(setting.getFaviconMediaId()));
    }

    public PublicHomeView getHome() {
        List<LatestUpdateView> updates = new ArrayList<>();
        for (TutorialPublishedPort.Chapter chapter : tutorials.latestPublishedChapters(LATEST_LIMIT)) {
            updates.add(new LatestUpdateView(
                    "TUTORIAL", chapter.id(), chapter.title(), null,
                    chapter.tutorialSlug(), chapter.chapterSlug(), formatUtc(chapter.updatedAt())));
        }
        for (BlogPublishedPort.Post post : blogs.latestPublished(LATEST_LIMIT)) {
            updates.add(new LatestUpdateView(
                    "BLOG", post.id(), post.title(), post.slug(), null, null, formatUtc(post.publishedAt())));
        }
        for (PortfolioPublishedPort.Project project : projects.latestPublished(LATEST_LIMIT)) {
            updates.add(new LatestUpdateView(
                    "PORTFOLIO", project.id(), project.title(), project.slug(), null, null, formatUtc(project.updatedAt())));
        }
        updates.sort(Comparator.comparing(LatestUpdateView::activityAt).reversed());
        List<LatestUpdateView> latest = updates.size() > LATEST_LIMIT
                ? new ArrayList<>(updates.subList(0, LATEST_LIMIT)) : updates;

        List<PortfolioPublishedPort.Featured> featuredRows = projects.featured(FEATURED_LIMIT);
        Map<Long, String> covers = media.publicUrls(featuredRows.stream()
                .map(PortfolioPublishedPort.Featured::coverMediaId).toList());
        List<FeaturedProjectView> featured = featuredRows.stream()
                .map(row -> new FeaturedProjectView(
                        row.id(), row.title(), row.slug(), row.summary(),
                        row.coverMediaId() == null ? null : covers.get(row.coverMediaId()),
                        row.projectStatus()))
                .toList();

        ProfilePreviewPort.Preview preview = profiles.preview();
        return SiteViewAssembler.home(latest, featured,
                new AboutPreviewView(preview.displayName(), preview.headline(), preview.bio()));
    }

    public AdminSiteSettingsView getAdminSettings() {
        SiteSetting setting = settings.findSingleton();
        if (setting == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SITE_SETTING_NOT_FOUND",
                    "Site settings not found", "The singleton site settings row is missing.");
        }
        return SiteViewAssembler.admin(setting);
    }

    private String formatUtc(LocalDateTime utc) {
        return siteSettingsTimezone.atSite(utc).format(ISO_OFFSET);
    }
}
