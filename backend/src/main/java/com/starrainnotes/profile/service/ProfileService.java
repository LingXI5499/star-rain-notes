package com.starrainnotes.profile.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.blog.entity.BlogPost;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.portfolio.entity.PortfolioProject;
import com.starrainnotes.portfolio.mapper.PortfolioProjectMapper;
import com.starrainnotes.profile.dto.AdminAboutView;
import com.starrainnotes.profile.dto.PublicAboutView;
import com.starrainnotes.profile.dto.SelectedContentView;
import com.starrainnotes.profile.dto.UpdateAboutRequest;
import com.starrainnotes.profile.dto.UpdateSelectedContentRequest;
import com.starrainnotes.profile.entity.Profile;
import com.starrainnotes.profile.mapper.ProfileMapper;
import com.starrainnotes.tutorial.entity.Tutorial;
import com.starrainnotes.tutorial.mapper.TutorialMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * About module (01 §6, 04 §14): profile singleton + selected content.
 *
 * <p>Selected content: each type ≤ 3, input order = display order, ids
 * deduped, all ids must exist; Draft/Withdrawn may be selected. The public
 * view only outputs PUBLISHED content; withdrawing keeps the relation and
 * republishing restores it automatically; deleting content removes the
 * relation via FK CASCADE. Profile updates and selected-content updates are
 * transactional.</p>
 */
@Service
public class ProfileService {

    private static final int MAX_SELECTED_PER_TYPE = 3;
    private static final String PUBLISHED = "PUBLISHED";

    private final ProfileMapper profileMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final TutorialMapper tutorialMapper;
    private final BlogPostMapper blogPostMapper;
    private final PortfolioProjectMapper portfolioProjectMapper;
    private final JdbcTemplate jdbc;

    public ProfileService(ProfileMapper profileMapper, MediaAssetMapper mediaAssetMapper,
                          TutorialMapper tutorialMapper, BlogPostMapper blogPostMapper,
                          PortfolioProjectMapper portfolioProjectMapper, JdbcTemplate jdbc) {
        this.profileMapper = profileMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.tutorialMapper = tutorialMapper;
        this.blogPostMapper = blogPostMapper;
        this.portfolioProjectMapper = portfolioProjectMapper;
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------
    // public / admin views
    // ---------------------------------------------------------------

    public PublicAboutView getPublic() {
        Profile profile = requireProfile();
        List<SelectedRow> rows = loadSelectedRows();
        List<SelectedContentView> tutorials = new ArrayList<>();
        List<SelectedContentView> blogs = new ArrayList<>();
        List<SelectedContentView> projects = new ArrayList<>();
        for (SelectedRow row : rows) {
            if (!PUBLISHED.equals(row.publishStatus())) {
                continue;
            }
            SelectedContentView view = new SelectedContentView(row.contentId(), row.title(), row.slug());
            switch (row.type()) {
                case "TUTORIAL" -> tutorials.add(view);
                case "BLOG" -> blogs.add(view);
                default -> projects.add(view);
            }
        }
        return new PublicAboutView(
                profile.getDisplayName(), profile.getHeadline(), profile.getBio(),
                mediaUrl(profile.getAvatarMediaId()), profile.getGithubUrl(), profile.getPublicEmail(),
                mediaUrl(profile.getResumeMediaId()),
                profile.getCurrentFocus() == null ? List.of() : profile.getCurrentFocus(),
                profile.getTechnicalDirectionMarkdown(), profile.getJourneyMarkdown(),
                tutorials, blogs, projects);
    }

    public AdminAboutView getAdmin() {
        Profile profile = requireProfile();
        List<SelectedRow> rows = loadSelectedRows();
        List<Long> tutorialIds = new ArrayList<>();
        List<Long> blogIds = new ArrayList<>();
        List<Long> projectIds = new ArrayList<>();
        for (SelectedRow row : rows) {
            switch (row.type()) {
                case "TUTORIAL" -> tutorialIds.add(row.contentId());
                case "BLOG" -> blogIds.add(row.contentId());
                default -> projectIds.add(row.contentId());
            }
        }
        return new AdminAboutView(
                profile.getId(), profile.getDisplayName(), profile.getHeadline(), profile.getBio(),
                profile.getAvatarMediaId(), profile.getGithubUrl(), profile.getPublicEmail(),
                profile.getResumeMediaId(),
                profile.getCurrentFocus() == null ? List.of() : profile.getCurrentFocus(),
                profile.getTechnicalDirectionMarkdown(), profile.getJourneyMarkdown(),
                tutorialIds, blogIds, projectIds);
    }

    // ---------------------------------------------------------------
    // updates
    // ---------------------------------------------------------------

    @Transactional
    public AdminAboutView update(UpdateAboutRequest request) {
        validateMedia(request.avatarMediaId(), "avatar");
        validateMedia(request.resumeMediaId(), "resume");
        Profile profile = requireProfile();
        profile.setDisplayName(request.displayName());
        profile.setHeadline(request.headline());
        profile.setBio(request.bio());
        profile.setAvatarMediaId(request.avatarMediaId());
        profile.setGithubUrl(request.githubUrl());
        profile.setPublicEmail(request.publicEmail());
        profile.setResumeMediaId(request.resumeMediaId());
        profile.setCurrentFocus(request.currentFocus() == null ? List.of() : request.currentFocus());
        profile.setTechnicalDirectionMarkdown(request.technicalDirectionMarkdown());
        profile.setJourneyMarkdown(request.journeyMarkdown());
        profileMapper.updateById(profile);
        return getAdmin();
    }

    @Transactional
    public AdminAboutView updateSelectedContent(UpdateSelectedContentRequest request) {
        List<Long> tutorials = dedupe(request.tutorialIds());
        List<Long> blogs = dedupe(request.blogPostIds());
        List<Long> projects = dedupe(request.portfolioProjectIds());
        if (tutorials.size() > MAX_SELECTED_PER_TYPE
                || blogs.size() > MAX_SELECTED_PER_TYPE
                || projects.size() > MAX_SELECTED_PER_TYPE) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SELECTED_LIMIT_EXCEEDED",
                    "Selected limit exceeded", "At most " + MAX_SELECTED_PER_TYPE + " items per type can be selected.");
        }
        validateExist(tutorials, tutorialMapper);
        validateExist(blogs, blogPostMapper);
        validateExist(projects, portfolioProjectMapper);

        jdbc.update("DELETE FROM profile_selected_content WHERE profile_id = 1");
        int order = 10;
        for (Long id : tutorials) {
            jdbc.update("INSERT INTO profile_selected_content (profile_id, tutorial_id, sort_order) VALUES (1, ?, ?)",
                    id, order);
            order += 10;
        }
        for (Long id : blogs) {
            jdbc.update("INSERT INTO profile_selected_content (profile_id, blog_post_id, sort_order) VALUES (1, ?, ?)",
                    id, order);
            order += 10;
        }
        for (Long id : projects) {
            jdbc.update("INSERT INTO profile_selected_content (profile_id, portfolio_project_id, sort_order) VALUES (1, ?, ?)",
                    id, order);
            order += 10;
        }
        return getAdmin();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private record SelectedRow(String type, Long contentId, String title, String slug,
                               String publishStatus, Integer sortOrder) {
    }

    private List<SelectedRow> loadSelectedRows() {
        return jdbc.query("""
                SELECT 'TUTORIAL' AS type, sc.tutorial_id AS content_id, t.title, t.slug,
                       t.publish_status, sc.sort_order
                FROM profile_selected_content sc JOIN tutorial t ON t.id = sc.tutorial_id
                WHERE sc.profile_id = 1 AND sc.tutorial_id IS NOT NULL
                UNION ALL
                SELECT 'BLOG', sc.blog_post_id, b.title, b.slug, b.publish_status, sc.sort_order
                FROM profile_selected_content sc JOIN blog_post b ON b.id = sc.blog_post_id
                WHERE sc.profile_id = 1 AND sc.blog_post_id IS NOT NULL
                UNION ALL
                SELECT 'PORTFOLIO', sc.portfolio_project_id, p.title, p.slug, p.publish_status, sc.sort_order
                FROM profile_selected_content sc JOIN portfolio_project p ON p.id = sc.portfolio_project_id
                WHERE sc.profile_id = 1 AND sc.portfolio_project_id IS NOT NULL
                ORDER BY sort_order
                """, (rs, rowNum) -> new SelectedRow(
                rs.getString("type"), rs.getLong("content_id"), rs.getString("title"), rs.getString("slug"),
                rs.getString("publish_status"), rs.getInt("sort_order")));
    }

    private Profile requireProfile() {
        Profile profile = profileMapper.selectById(1);
        if (profile == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                    "Profile not found", "The profile singleton row is missing.");
        }
        return profile;
    }

    private String mediaUrl(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        return media == null ? null : media.getPublicUrl();
    }

    private void validateMedia(Long mediaId, String field) {
        if (mediaId == null) {
            return;
        }
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        if (media == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_NOT_FOUND",
                    "Media not found", "The referenced media asset does not exist.");
        }
        if ("avatar".equals(field) && !"IMAGE".equals(media.getAssetType())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_TYPE_INVALID",
                    "Avatar must be an image", "The avatar must reference an IMAGE asset.");
        }
        if ("resume".equals(field)) {
            boolean pdf = "DOCUMENT".equals(media.getAssetType())
                    && "pdf".equalsIgnoreCase(media.getExtension())
                    && media.getMimeType() != null
                    && media.getMimeType().toLowerCase().contains("pdf");
            if (!pdf) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_TYPE_INVALID",
                        "Resume must be a PDF document", "The resume must reference a PDF DOCUMENT asset.");
            }
        }
    }

    private void validateExist(List<Long> ids, BaseMapper<?> mapper) {
        if (ids.isEmpty()) {
            return;
        }
        if (mapper.selectBatchIds(ids).size() != ids.size()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTENT_NOT_FOUND",
                    "Selected content not found", "One or more selected content ids do not exist.");
        }
    }

    private List<Long> dedupe(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }
}
