package com.starrainnotes.profile.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.profile.dto.AdminAboutView;
import com.starrainnotes.profile.dto.PublicAboutView;
import com.starrainnotes.profile.dto.SelectedContentView;
import com.starrainnotes.profile.entity.Profile;
import com.starrainnotes.profile.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Read-side profile singleton and selected-content projections. */
@Service
@RequiredArgsConstructor
public class ProfileQueryService {

    private static final String PUBLISHED = "PUBLISHED";

    private final ProfileMapper profileMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final JdbcTemplate jdbc;

    public PublicAboutView getPublic() {
        Profile profile = requireProfile();
        List<SelectedContentView> tutorials = new ArrayList<>();
        List<SelectedContentView> blogs = new ArrayList<>();
        List<SelectedContentView> projects = new ArrayList<>();
        for (SelectedRow row : loadSelectedRows()) {
            if (!PUBLISHED.equals(row.publishStatus())) continue;
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
                profile.getTechnicalDirectionMarkdown(), profile.getJourneyMarkdown(), tutorials, blogs, projects);
    }

    public AdminAboutView getAdmin() {
        Profile profile = requireProfile();
        List<Long> tutorials = new ArrayList<>();
        List<Long> blogs = new ArrayList<>();
        List<Long> projects = new ArrayList<>();
        for (SelectedRow row : loadSelectedRows()) {
            switch (row.type()) {
                case "TUTORIAL" -> tutorials.add(row.contentId());
                case "BLOG" -> blogs.add(row.contentId());
                default -> projects.add(row.contentId());
            }
        }
        return new AdminAboutView(
                profile.getId(), profile.getDisplayName(), profile.getHeadline(), profile.getBio(),
                profile.getAvatarMediaId(), profile.getGithubUrl(), profile.getPublicEmail(), profile.getResumeMediaId(),
                profile.getCurrentFocus() == null ? List.of() : profile.getCurrentFocus(),
                profile.getTechnicalDirectionMarkdown(), profile.getJourneyMarkdown(), tutorials, blogs, projects);
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
                """, (resultSet, rowNum) -> new SelectedRow(
                resultSet.getString("type"), resultSet.getLong("content_id"), resultSet.getString("title"),
                resultSet.getString("slug"), resultSet.getString("publish_status"), resultSet.getInt("sort_order")));
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
        if (mediaId == null) return null;
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        return media == null ? null : media.getPublicUrl();
    }

    private record SelectedRow(String type, Long contentId, String title, String slug,
                               String publishStatus, Integer sortOrder) {
    }
}
