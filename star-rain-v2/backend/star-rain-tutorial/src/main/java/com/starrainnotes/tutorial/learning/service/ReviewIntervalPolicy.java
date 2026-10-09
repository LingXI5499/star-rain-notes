package com.starrainnotes.tutorial.learning.service;

import com.starrainnotes.tutorial.learning.enumeration.RecallRating;
import com.starrainnotes.tutorial.learning.vo.ReviewIntervalDecision;

public interface ReviewIntervalPolicy {
    ReviewIntervalDecision next(int stepIndex, int currentIntervalDays, RecallRating rating);
}
