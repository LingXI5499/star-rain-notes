package com.starrainnotes.profile.dto;

import java.util.List;

/**
 * Public About view. Only PUBLISHED selected content is exposed; avatar/resume
 * are public URLs (never storage paths or media ids).
 */
public record PublicAboutView(
        String displayName,
        String headline,
        String bio,
        String avatarUrl,
        String githubUrl,
        String publicEmail,
        String resumeUrl,
        List<String> currentFocus,
        String technicalDirectionMarkdown,
        String journeyMarkdown,
        List<SelectedContentView> selectedTutorials,
        List<SelectedContentView> selectedBlogs,
        List<SelectedContentView> selectedProjects) {
}
