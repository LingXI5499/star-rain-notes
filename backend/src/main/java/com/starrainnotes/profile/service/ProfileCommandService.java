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
import com.starrainnotes.profile.dto.UpdateAboutRequest;
import com.starrainnotes.profile.dto.UpdateSelectedContentRequest;
import com.starrainnotes.profile.entity.Profile;
import com.starrainnotes.profile.mapper.ProfileMapper;
import com.starrainnotes.tutorial.entity.Tutorial;
import com.starrainnotes.tutorial.mapper.TutorialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Write-side profile singleton and selected-content management.
 */
@Service
@RequiredArgsConstructor
public class ProfileCommandService {

    private static final int MAX_SELECTED_PER_TYPE = 3;

    private final ProfileMapper profileMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final TutorialMapper tutorialMapper;
    private final BlogPostMapper blogPostMapper;
    private final PortfolioProjectMapper portfolioProjectMapper;
    private final JdbcTemplate jdbc;
    private final ProfileQueryService queryService;

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
        return queryService.getAdmin();
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
        return queryService.getAdmin();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Profile requireProfile() {
        Profile profile = profileMapper.selectById(1);
        if (profile == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                    "Profile not found", "The profile singleton row is missing.");
        }
        return profile;
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
