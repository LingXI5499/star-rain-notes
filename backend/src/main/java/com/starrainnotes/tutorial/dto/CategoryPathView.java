package com.starrainnotes.tutorial.dto;

/**
 * One step of the tutorial's category path (root first).
 */
public record CategoryPathView(
        Long id,
        String name,
        String slug) {
}
