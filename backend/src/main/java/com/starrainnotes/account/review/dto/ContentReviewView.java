package com.starrainnotes.account.review.dto;

import java.util.Map;

public record ContentReviewView(
        Long id,
        String contentType,
        Long contentId,
        String actionType,
        String title,
        Map<String, Object> payload,
        String status,
        Long submittedBy,
        Long reviewedBy,
        String reviewNote,
        String createdAt,
        String updatedAt,
        String reviewedAt) {
}
