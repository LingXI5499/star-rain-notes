package com.starrainnotes.english.dto;

/**
 * English overview view (public + admin GET share this shape — all fields are
 * public-facing). currentStage is read-only in V1.
 */
public record EnglishView(
        Integer id,
        String title,
        String subtitle,
        String introduction,
        String currentStage,
        String roadmapMarkdown) {
}
