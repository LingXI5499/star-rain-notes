package com.starrainnotes.tutorial.service;

import com.starrainnotes.tutorial.enumeration.RecallRating;
import com.starrainnotes.tutorial.vo.ReviewIntervalDecision;

public interface ReviewIntervalPolicy {
    ReviewIntervalDecision next(int stepIndex, int currentIntervalDays, RecallRating rating);
}
