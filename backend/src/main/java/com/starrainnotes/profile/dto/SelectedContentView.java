package com.starrainnotes.profile.dto;

import java.util.List;

/**
 * One selected content reference inside the public About view (published only).
 */
public record SelectedContentView(
        Long id,
        String title,
        String slug) {
}
