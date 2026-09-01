package com.starrainnotes.tutorial.dto;

/**
 * First published chapter of a tutorial (null when none is public yet).
 */
public record FirstChapterView(
        Long id,
        String title,
        String slug) {
}
