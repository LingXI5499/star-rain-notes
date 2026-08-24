package com.starrainnotes.search.dto;

/**
 * Search counts per source, based on q and ignoring the current type filter.
 */
public record SearchCountsView(
        long tutorial,
        long chapter,
        long blog,
        long portfolio,
        long grammar) {
}
