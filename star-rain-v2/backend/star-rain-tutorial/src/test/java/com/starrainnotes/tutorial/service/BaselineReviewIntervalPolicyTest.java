package com.starrainnotes.tutorial.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.starrainnotes.tutorial.enumeration.RecallRating;
import com.starrainnotes.tutorial.properties.LearningReviewProperties;
import com.starrainnotes.tutorial.service.impl.BaselineReviewIntervalPolicy;
import com.starrainnotes.tutorial.vo.ReviewIntervalDecision;
import java.util.List;
import org.junit.jupiter.api.Test;

class BaselineReviewIntervalPolicyTest {
    private BaselineReviewIntervalPolicy policy(List<Integer> intervals) {
        LearningReviewProperties properties = new LearningReviewProperties();
        properties.setBaseIntervalDays(intervals);
        return new BaselineReviewIntervalPolicy(properties);
    }

    @Test
    void advancesByRecallQualityAndCapsAtLastStep() {
        BaselineReviewIntervalPolicy policy = policy(List.of(1, 3, 7, 14, 30, 60));
        assertDecision(policy.next(1, 3, RecallRating.NORMAL), 2, 7);
        assertDecision(policy.next(1, 3, RecallRating.EASY), 3, 14);
        assertDecision(policy.next(5, 60, RecallRating.EASY), 5, 60);
    }

    @Test
    void forgottenCardRetreatsAndHardCardGetsShorterGrowth() {
        BaselineReviewIntervalPolicy policy = policy(List.of(1, 3, 7, 14, 30, 60));
        assertDecision(policy.next(4, 30, RecallRating.FORGOT), 2, 7);
        assertDecision(policy.next(0, 1, RecallRating.FORGOT), 0, 1);
        assertDecision(policy.next(2, 7, RecallRating.HARD), 2, 11);
    }

    @Test
    void invalidScheduleConfigurationFailsClosed() {
        assertThrows(IllegalStateException.class,
                () -> policy(List.of()).next(0, 1, RecallRating.NORMAL));
        assertThrows(IllegalStateException.class,
                () -> policy(List.of(0, 3)).next(0, 1, RecallRating.NORMAL));
    }

    private void assertDecision(ReviewIntervalDecision decision, int step, int days) {
        assertEquals(step, decision.getStepIndex());
        assertEquals(days, decision.getIntervalDays());
    }
}
