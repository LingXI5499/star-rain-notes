package com.starrainnotes.profile.dto;

import java.util.List;

/**
 * Admin About view — includes Draft/Withdrawn selections (ids in display
 * order) and media ids for editing.
 */
public record AdminAboutView(
        Integer id,
        String displayName,
        String headline,
        String bio,
        Long avatarMediaId,
        String githubUrl,
        String publicEmail,
        Long resumeMediaId,
        List<String> currentFocus,
        String technicalDirectionMarkdown,
        String journeyMarkdown,
        List<Long> selectedTutorialIds,
        List<Long> selectedBlogPostIds,
        List<Long> selectedPortfolioProjectIds) {
}
