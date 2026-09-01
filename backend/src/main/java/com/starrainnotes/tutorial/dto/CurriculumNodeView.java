package com.starrainnotes.tutorial.dto;

import java.util.List;

/**
 * One node of the public curriculum tree: all GROUPs plus PUBLISHED chapters.
 */
public record CurriculumNodeView(
        Long id,
        String type,
        String title,
        String slug,
        List<CurriculumNodeView> children) {
}
