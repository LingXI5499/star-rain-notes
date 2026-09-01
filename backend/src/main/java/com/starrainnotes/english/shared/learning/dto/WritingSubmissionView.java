package com.starrainnotes.english.shared.learning.dto;

import java.math.BigDecimal;

public record WritingSubmissionView(
        Long id, Long promptId, String bodyText, int wordCount, String status,
        BigDecimal selfScore, String submittedAt, String updatedAt) {
}
