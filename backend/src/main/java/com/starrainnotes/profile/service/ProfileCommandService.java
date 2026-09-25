package com.starrainnotes.profile.service;

import com.starrainnotes.blog.api.BlogLookupPort;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.api.MediaAssetPort;
import com.starrainnotes.media.api.MediaAssetPort.MediaFile;
import com.starrainnotes.portfolio.api.PortfolioLookupPort;
import com.starrainnotes.profile.ProfileChangedEvent;
import com.starrainnotes.profile.dto.AdminAboutView;
import com.starrainnotes.profile.dto.UpdateAboutRequest;
import com.starrainnotes.profile.dto.UpdateSelectedContentRequest;
import com.starrainnotes.profile.entity.Profile;
import com.starrainnotes.profile.entity.ProfileSelectedContent;
import com.starrainnotes.profile.mapper.ProfileMapper;
import com.starrainnotes.tutorial.api.TutorialLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
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
    private final MediaAssetPort media;
    private final TutorialLookupPort tutorials;
    private final BlogLookupPort blogs;
    private final PortfolioLookupPort projects;
    private final ProfileSelectedContentRepository selected;
    private final ProfileQueryService queryService;
    private final ApplicationEventPublisher events;

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
        events.publishEvent(new ProfileChangedEvent());
        return queryService.getAdmin();
    }

    @Transactional
    public AdminAboutView updateSelectedContent(UpdateSelectedContentRequest request) {
        List<Long> tutorialIds = dedupe(request.tutorialIds());
        List<Long> blogIds = dedupe(request.blogPostIds());
        List<Long> projectIds = dedupe(request.portfolioProjectIds());
        if (tutorialIds.size() > MAX_SELECTED_PER_TYPE
                || blogIds.size() > MAX_SELECTED_PER_TYPE
                || projectIds.size() > MAX_SELECTED_PER_TYPE) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SELECTED_LIMIT_EXCEEDED",
                    "Selected limit exceeded", "At most " + MAX_SELECTED_PER_TYPE + " items per type can be selected.");
        }
        if (!tutorials.containsAll(tutorialIds) || !blogs.containsAll(blogIds) || !projects.containsAll(projectIds)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTENT_NOT_FOUND",
                    "Selected content not found", "One or more selected content ids do not exist.");
        }
        List<ProfileSelectedContent> rows = new ArrayList<>();
        int order = 10;
        for (Long id : tutorialIds) {
            rows.add(selectedRow(id, null, null, order));
            order += 10;
        }
        for (Long id : blogIds) {
            rows.add(selectedRow(null, id, null, order));
            order += 10;
        }
        for (Long id : projectIds) {
            rows.add(selectedRow(null, null, id, order));
            order += 10;
        }
        selected.replace(rows);
        events.publishEvent(new ProfileChangedEvent());
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
        MediaFile file = media.file(mediaId);
        if (file == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_NOT_FOUND",
                    "Media not found", "The referenced media asset does not exist.");
        }
        if ("avatar".equals(field) && !"IMAGE".equals(file.assetType())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_TYPE_INVALID",
                    "Avatar must be an image", "The avatar must reference an IMAGE asset.");
        }
        if ("resume".equals(field)) {
            boolean pdf = "DOCUMENT".equals(file.assetType())
                    && "pdf".equalsIgnoreCase(file.extension())
                    && file.mimeType() != null
                    && file.mimeType().toLowerCase().contains("pdf");
            if (!pdf) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_TYPE_INVALID",
                        "Resume must be a PDF document", "The resume must reference a PDF DOCUMENT asset.");
            }
        }
    }

    private ProfileSelectedContent selectedRow(Long tutorialId, Long blogId, Long projectId, int order) {
        ProfileSelectedContent row = new ProfileSelectedContent();
        row.setProfileId(1);
        row.setTutorialId(tutorialId);
        row.setBlogPostId(blogId);
        row.setPortfolioProjectId(projectId);
        row.setSortOrder(order);
        return row;
    }

    private List<Long> dedupe(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }
}
