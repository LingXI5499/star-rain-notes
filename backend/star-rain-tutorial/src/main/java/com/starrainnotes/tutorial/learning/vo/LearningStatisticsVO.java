package com.starrainnotes.tutorial.learning.vo;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningStatisticsVO {
    private long startedChapters;
    private long completedChapters;
    private long studySecondsTotal;
    private long activePlans;
    private long todayStudyTasks;
    private long dueReviews;
    private long completedReviews;
    private Map<String, Long> masteryDistribution;
}
