package com.starrainnotes.tutorial.service;

import com.starrainnotes.tutorial.dto.LearningAnswerDTO;
import com.starrainnotes.tutorial.vo.LearningAnswerVO;

public interface LearningAnswerService {
    LearningAnswerVO answer(Long questionId, LearningAnswerDTO request);
    LearningAnswerVO ownAnswer(Long questionId);
    String referenceAnswer(Long questionId);
}
