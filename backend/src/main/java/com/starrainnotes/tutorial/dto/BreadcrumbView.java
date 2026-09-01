package com.starrainnotes.tutorial.dto;

/**
 * One breadcrumb step of a public chapter (TUTORIAL / GROUP / CHAPTER).
 */
public record BreadcrumbView(
        String type,
        Long id,
        String title,
        String slug) {
}
