package com.starrainnotes.site.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.site.dto.AboutPreviewView;
import com.starrainnotes.site.dto.AdminSiteSettingsView;
import com.starrainnotes.site.dto.FeaturedProjectView;
import com.starrainnotes.site.dto.LatestUpdateView;
import com.starrainnotes.site.dto.PublicHomeView;
import com.starrainnotes.site.dto.PublicSiteView;
import com.starrainnotes.site.dto.UpdateSiteSettingsRequest;
import com.starrainnotes.site.entity.SiteSetting;
import com.starrainnotes.site.mapper.SiteSettingMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
public class SiteService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final JdbcTemplate jdbc;
    private final SiteSettingMapper siteSettingMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final SiteSettingsTimezone siteSettingsTimezone;

    public SiteService(JdbcTemplate jdbc, SiteSettingMapper siteSettingMapper,
                       MediaAssetMapper mediaAssetMapper, SiteSettingsTimezone siteSettingsTimezone) {
        this.jdbc = jdbc;
        this.siteSettingMapper = siteSettingMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.siteSettingsTimezone = siteSettingsTimezone;
    }

    // ---------------------------------------------------------------
    // public
    // ---------------------------------------------------------------

    public PublicSiteView getPublicSite() {
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT s.site_name, s.tagline, s.site_url, s.footer_text, s.github_url,
                       s.default_seo_description, s.timezone,
                       m_logo.public_url AS logo_url, m_fav.public_url AS favicon_url
                FROM site_setting s
                LEFT JOIN media_asset m_logo ON m_logo.id = s.logo_media_id
                LEFT JOIN media_asset m_fav  ON m_fav.id  = s.favicon_media_id
                WHERE s.id = 1
                """);
        return new PublicSiteView(
                str(row, "site_name"),
                str(row, "tagline"),
                str(row, "site_url"),
                str(row, "footer_text"),
                str(row, "github_url"),
                str(row, "default_seo_description"),
                str(row, "timezone"),
                str(row, "logo_url"),
                str(row, "favicon_url"));
    }

    public PublicHomeView getHome() {
        List<LatestUpdateView> updates = new ArrayList<>();
        updates.addAll(publishedChapters());
        updates.addAll(publishedBlogs());
        updates.addAll(publishedPortfolio());
        updates.sort(Comparator.comparing(LatestUpdateView::activityAt).reversed());
        List<LatestUpdateView> latest = updates.size() > 6 ? new ArrayList<>(updates.subList(0, 6)) : updates;

        List<FeaturedProjectView> featured = jdbc.query("""
                SELECT p.id, p.title, p.slug, p.summary, p.project_status,
                       m.public_url AS cover_url
                FROM portfolio_project p
                LEFT JOIN media_asset m ON m.id = p.cover_media_id
                WHERE p.publish_status = 'PUBLISHED' AND p.featured = 1
                ORDER BY p.sort_order ASC, p.id ASC
                LIMIT 3
                """, (rs, rowNum) -> new FeaturedProjectView(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("slug"),
                rs.getString("summary"),
                rs.getString("cover_url"),
                rs.getString("project_status")));

        AboutPreviewView preview = jdbc.query("""
                SELECT display_name, headline, bio
                FROM profile
                WHERE id = 1
                """, rs -> rs.next()
                ? new AboutPreviewView(rs.getString("display_name"), rs.getString("headline"), rs.getString("bio"))
                : new AboutPreviewView(null, null, null));

        return new PublicHomeView(latest, featured, preview);
    }

    private List<LatestUpdateView> publishedChapters() {
        return jdbc.query("""
                SELECT n.id, n.title, n.slug AS chapter_slug, n.updated_at,
                       t.slug AS tutorial_slug
                FROM tutorial_node n
                JOIN tutorial t ON t.id = n.tutorial_id
                WHERE t.publish_status = 'PUBLISHED'
                  AND n.node_type = 'CHAPTER'
                  AND n.publish_status = 'PUBLISHED'
                ORDER BY n.updated_at DESC
                LIMIT 6
                """, (rs, rowNum) -> new LatestUpdateView(
                "TUTORIAL",
                rs.getLong("id"),
                rs.getString("title"),
                null,
                rs.getString("tutorial_slug"),
                rs.getString("chapter_slug"),
                formatUtc(rs.getTimestamp("updated_at"))));
    }

    private List<LatestUpdateView> publishedBlogs() {
        return jdbc.query("""
                SELECT id, title, slug, published_at
                FROM blog_post
                WHERE publish_status = 'PUBLISHED'
                ORDER BY published_at DESC
                LIMIT 6
                """, (rs, rowNum) -> new LatestUpdateView(
                "BLOG",
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("slug"),
                null,
                null,
                formatUtc(rs.getTimestamp("published_at"))));
    }

    private List<LatestUpdateView> publishedPortfolio() {
        return jdbc.query("""
                SELECT id, title, slug, updated_at
                FROM portfolio_project
                WHERE publish_status = 'PUBLISHED'
                ORDER BY updated_at DESC
                LIMIT 6
                """, (rs, rowNum) -> new LatestUpdateView(
                "PORTFOLIO",
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("slug"),
                null,
                null,
                formatUtc(rs.getTimestamp("updated_at"))));
    }

    // ---------------------------------------------------------------
    // admin
    // ---------------------------------------------------------------

    public AdminSiteSettingsView getAdminSettings() {
        SiteSetting setting = siteSettingMapper.selectById(1);
        if (setting == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SITE_SETTING_NOT_FOUND",
                    "Site settings not found", "The singleton site settings row is missing.");
        }
        return toView(setting);
    }

    public AdminSiteSettingsView updateAdminSettings(UpdateSiteSettingsRequest request) {
        validateTimezone(request.timezone());
        validateImageMedia(request.logoMediaId(), "logo");
        validateImageMedia(request.faviconMediaId(), "favicon");

        SiteSetting setting = siteSettingMapper.selectById(1);
        if (setting == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SITE_SETTING_NOT_FOUND",
                    "Site settings not found", "The singleton site settings row is missing.");
        }
        setting.setSiteName(request.siteName());
        setting.setTagline(request.tagline());
        setting.setSiteUrl(request.siteUrl());
        setting.setFooterText(request.footerText());
        setting.setGithubUrl(request.githubUrl());
        if (request.defaultSeoDescription() != null) {
            setting.setDefaultSeoDescription(request.defaultSeoDescription());
        }
        setting.setTimezone(request.timezone());
        setting.setLogoMediaId(request.logoMediaId());
        setting.setFaviconMediaId(request.faviconMediaId());
        siteSettingMapper.updateById(setting);
        return toView(setting);
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TIMEZONE",
                    "Invalid timezone", "The timezone must be a valid IANA zone id.");
        }
    }

    private void validateImageMedia(Long mediaId, String field) {
        if (mediaId == null) {
            return;
        }
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        if (media == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_NOT_FOUND",
                    "Media not found", "The referenced media asset does not exist.");
        }
        if (!"IMAGE".equals(media.getAssetType())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_TYPE_INVALID",
                    "Only IMAGE media allowed",
                    "The " + field + " media id must reference an IMAGE asset, got " + media.getAssetType() + ".");
        }
    }

    private AdminSiteSettingsView toView(SiteSetting s) {
        return new AdminSiteSettingsView(
                s.getId(), s.getSiteName(), s.getTagline(), s.getSiteUrl(), s.getFooterText(),
                s.getGithubUrl(), s.getDefaultSeoDescription(), s.getTimezone(),
                s.getLogoMediaId(), s.getFaviconMediaId());
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private ZoneId siteZone() {
        return ZoneId.of(siteSettingsTimezone.get());
    }

    private String formatUtc(Timestamp utcTimestamp) {
        LocalDateTime utc = utcTimestamp.toLocalDateTime();
        return ZonedDateTime.of(utc, ZoneOffset.UTC)
                .withZoneSameInstant(siteZone())
                .format(ISO_OFFSET);
    }

    private String str(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : value.toString();
    }
}
