package com.starrainnotes.site.dto;

/**
 * Home "About Preview" — public profile teaser (all fields nullable).
 */
public record AboutPreviewView(
        String displayName,
        String headline,
        String bio) {
}
