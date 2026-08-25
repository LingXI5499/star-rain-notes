package com.starrainnotes.english.shared.learning.dto;

import java.util.List;
import java.util.Map;

public record LearningSummaryView(
        long total, long inProgress, long completed, long dueForReview,
        Map<String, Long> completedByType, List<LearningRecordView> recent) {
}
