package com.starrainnotes.tutorial.learning.service;

import com.starrainnotes.tutorial.learning.dto.LearningAnswerDTO;
import com.starrainnotes.tutorial.learning.vo.LearningAnswerVO;

public interface LearningAnswerService {
    LearningAnswerVO answer(Long questionId, LearningAnswerDTO request);
    LearningAnswerVO ownAnswer(Long questionId);
    String referenceAnswer(Long questionId);
}
