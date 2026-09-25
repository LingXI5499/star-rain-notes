package com.starrainnotes.profile.service;

import com.starrainnotes.blog.api.BlogLookupPort;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.api.MediaAssetPort;
import com.starrainnotes.portfolio.api.PortfolioLookupPort;
import com.starrainnotes.profile.assembler.ProfileViewAssembler;
import com.starrainnotes.profile.dto.AdminAboutView;
import com.starrainnotes.profile.dto.PublicAboutView;
import com.starrainnotes.profile.dto.SelectedContentView;
import com.starrainnotes.profile.entity.Profile;
import com.starrainnotes.profile.entity.ProfileSelectedContent;
import com.starrainnotes.profile.mapper.ProfileMapper;
import com.starrainnotes.tutorial.api.TutorialLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Read-side profile singleton and selected-content projections. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileQueryService {

    private static final String PUBLISHED = "PUBLISHED";

    private final ProfileMapper profileMapper;
    private final MediaAssetPort media;
    private final ProfileSelectedContentRepository selected;
    private final TutorialLookupPort tutorials;
    private final BlogLookupPort blogs;
    private final PortfolioLookupPort projects;

    public PublicAboutView getPublic() {
        Profile profile = requireProfile();
        List<SelectedContentView> tutorials = new ArrayList<>();
        List<SelectedContentView> blogs = new ArrayList<>();
        List<SelectedContentView> projects = new ArrayList<>();
        Selection selection = loadSelection();
        for (TutorialLookupPort.Ref ref : selection.tutorials()) {
            if (PUBLISHED.equals(ref.publishStatus())) tutorials.add(new SelectedContentView(ref.id(), ref.title(), ref.slug()));
        }
        for (BlogLookupPort.Ref ref : selection.blogs()) {
            if (PUBLISHED.equals(ref.publishStatus())) blogs.add(new SelectedContentView(ref.id(), ref.title(), ref.slug()));
        }
        for (PortfolioLookupPort.Ref ref : selection.projects()) {
            if (PUBLISHED.equals(ref.publishStatus())) projects.add(new SelectedContentView(ref.id(), ref.title(), ref.slug()));
        }
        return ProfileViewAssembler.publicAbout(profile, mediaUrl(profile.getAvatarMediaId()),
                mediaUrl(profile.getResumeMediaId()), tutorials, blogs, projects);
    }

    public AdminAboutView getAdmin() {
        Profile profile = requireProfile();
        List<Long> tutorials = new ArrayList<>();
        List<Long> blogs = new ArrayList<>();
        List<Long> projects = new ArrayList<>();
        Selection selection = loadSelection();
        tutorials.addAll(selection.tutorials().stream().map(TutorialLookupPort.Ref::id).toList());
        blogs.addAll(selection.blogs().stream().map(BlogLookupPort.Ref::id).toList());
        projects.addAll(selection.projects().stream().map(PortfolioLookupPort.Ref::id).toList());
        return ProfileViewAssembler.admin(profile, tutorials, blogs, projects);
    }

    private Selection loadSelection() {
        List<Long> tutorialIds = new ArrayList<>();
        List<Long> blogIds = new ArrayList<>();
        List<Long> projectIds = new ArrayList<>();
        for (ProfileSelectedContent row : selected.list()) {
            if (row.getTutorialId() != null) tutorialIds.add(row.getTutorialId());
            else if (row.getBlogPostId() != null) blogIds.add(row.getBlogPostId());
            else if (row.getPortfolioProjectId() != null) projectIds.add(row.getPortfolioProjectId());
        }
        Map<Long, TutorialLookupPort.Ref> tutorialRefs = tutorials.findAll(tutorialIds).stream()
                .collect(Collectors.toMap(TutorialLookupPort.Ref::id, Function.identity()));
        Map<Long, BlogLookupPort.Ref> blogRefs = blogs.findAll(blogIds).stream()
                .collect(Collectors.toMap(BlogLookupPort.Ref::id, Function.identity()));
        Map<Long, PortfolioLookupPort.Ref> projectRefs = projects.findAll(projectIds).stream()
                .collect(Collectors.toMap(PortfolioLookupPort.Ref::id, Function.identity()));
        return new Selection(
                tutorialIds.stream().map(tutorialRefs::get).filter(java.util.Objects::nonNull).toList(),
                blogIds.stream().map(blogRefs::get).filter(java.util.Objects::nonNull).toList(),
                projectIds.stream().map(projectRefs::get).filter(java.util.Objects::nonNull).toList());
    }

    private record Selection(List<TutorialLookupPort.Ref> tutorials, List<BlogLookupPort.Ref> blogs,
                             List<PortfolioLookupPort.Ref> projects) {
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
        return media.publicUrl(mediaId);
    }

}
