package com.starrainnotes.profile.assembler;

import com.starrainnotes.profile.dto.AdminAboutView;
import com.starrainnotes.profile.dto.PublicAboutView;
import com.starrainnotes.profile.dto.SelectedContentView;
import com.starrainnotes.profile.entity.Profile;

import java.util.List;

public final class ProfileViewAssembler {
    private ProfileViewAssembler() {
    }

    public static PublicAboutView publicAbout(Profile profile, String avatarUrl, String resumeUrl,
                                              List<SelectedContentView> tutorials, List<SelectedContentView> blogs,
                                              List<SelectedContentView> projects) {
        return new PublicAboutView(profile.getDisplayName(), profile.getHeadline(), profile.getBio(), avatarUrl,
                profile.getGithubUrl(), profile.getPublicEmail(), resumeUrl,
                profile.getCurrentFocus() == null ? List.of() : profile.getCurrentFocus(),
                profile.getTechnicalDirectionMarkdown(), profile.getJourneyMarkdown(), tutorials, blogs, projects);
    }

    public static AdminAboutView admin(Profile profile, List<Long> tutorials, List<Long> blogs, List<Long> projects) {
        return new AdminAboutView(profile.getId(), profile.getDisplayName(), profile.getHeadline(), profile.getBio(),
                profile.getAvatarMediaId(), profile.getGithubUrl(), profile.getPublicEmail(), profile.getResumeMediaId(),
                profile.getCurrentFocus() == null ? List.of() : profile.getCurrentFocus(),
                profile.getTechnicalDirectionMarkdown(), profile.getJourneyMarkdown(), tutorials, blogs, projects);
    }
}
