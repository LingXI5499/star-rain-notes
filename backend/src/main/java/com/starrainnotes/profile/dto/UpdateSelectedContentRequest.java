package com.starrainnotes.profile.dto;

import java.util.List;

/**
 * PUT /api/v1/admin/about/selected-content request body (04 §14).
 * Each type ≤ 3; input order is the display order; ids are deduped; all ids
 * must exist; Draft/Withdrawn may be selected (public output filters them).
 */
public record UpdateSelectedContentRequest(
        List<Long> tutorialIds,
        List<Long> blogPostIds,
        List<Long> portfolioProjectIds) {
}
