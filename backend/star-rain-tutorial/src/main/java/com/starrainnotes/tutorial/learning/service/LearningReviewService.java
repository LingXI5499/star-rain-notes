package com.starrainnotes.tutorial.learning.service;

import com.starrainnotes.tutorial.learning.enumeration.RecallRating;
import com.starrainnotes.tutorial.learning.vo.MasteryVO;
import com.starrainnotes.tutorial.learning.vo.ReviewResultVO;
import com.starrainnotes.tutorial.learning.vo.ReviewTaskVO;
import java.util.List;

public interface LearningReviewService {
    void initializeChapter(Long accountId, Long chapterId);
    List<ReviewTaskVO> today();
    ReviewTaskVO task(Long taskId);
    String back(Long taskId);
    ReviewResultVO complete(Long taskId, RecallRating rating);
    List<MasteryVO> mastery();
    MasteryVO selfRating(Long cardId, String level);
}
