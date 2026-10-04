package com.starrainnotes.tutorial.service.impl;

import com.starrainnotes.tutorial.enumeration.RecallRating;
import com.starrainnotes.tutorial.properties.LearningReviewProperties;
import com.starrainnotes.tutorial.service.ReviewIntervalPolicy;
import com.starrainnotes.tutorial.vo.ReviewIntervalDecision;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BaselineReviewIntervalPolicy implements ReviewIntervalPolicy {
    private final LearningReviewProperties properties;

    @Override
    public ReviewIntervalDecision next(int stepIndex, int currentIntervalDays, RecallRating rating) {
        List<Integer> intervals = properties.getBaseIntervalDays();
        if (intervals == null || intervals.isEmpty() || intervals.stream().anyMatch(day -> day == null || day < 1)) {
            throw new IllegalStateException("复习间隔配置必须是正整数序列");
        }
        int last = intervals.size() - 1;
        int current = Math.max(0, Math.min(stepIndex, last));
        int nextStep;
        int nextDays;
        switch (rating) {
            case FORGOT -> {
                nextStep = Math.max(0, current - 2);
                nextDays = intervals.get(nextStep);
            }
            case HARD -> {
                nextStep = current;
                nextDays = Math.max(intervals.get(0), Math.min(intervals.get(last),
                        Math.max(currentIntervalDays + 1, (int) Math.ceil(currentIntervalDays * 1.5))));
            }
            case NORMAL -> {
                nextStep = Math.min(last, current + 1);
                nextDays = intervals.get(nextStep);
            }
            case EASY -> {
                nextStep = Math.min(last, current + 2);
                nextDays = intervals.get(nextStep);
            }
            default -> throw new IllegalArgumentException("未知回忆评价");
        }
        return ReviewIntervalDecision.builder().stepIndex(nextStep).intervalDays(nextDays).build();
    }
}
