package com.starrainnotes.profile.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * PUT /api/v1/admin/about request body.
 * Media rules: avatar must be IMAGE; resume must be a PDF DOCUMENT.
 */
public record UpdateAboutRequest(
        @Size(max = 100) String displayName,
        @Size(max = 255) String headline,
        @Size(max = 1000) String bio,
        Long avatarMediaId,
        @Size(max = 500) String githubUrl,
        @Size(max = 255) String publicEmail,
        Long resumeMediaId,
        List<String> currentFocus,
        String technicalDirectionMarkdown,
        String journeyMarkdown) {
}
