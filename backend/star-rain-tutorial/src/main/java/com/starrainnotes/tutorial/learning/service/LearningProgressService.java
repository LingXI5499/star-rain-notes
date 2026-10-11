package com.starrainnotes.tutorial.learning.service;

import com.starrainnotes.tutorial.learning.dto.LearningProgressDTO;
import com.starrainnotes.tutorial.learning.vo.LearningProgressVO;
import com.starrainnotes.tutorial.learning.vo.LearningTutorialProgressVO;
import java.util.List;

public interface LearningProgressService {
    LearningProgressVO save(Long chapterId, LearningProgressDTO request);
    LearningProgressVO complete(Long chapterId);
    LearningProgressVO chapter(Long chapterId);
    LearningTutorialProgressVO tutorial(Long tutorialId);
    List<LearningProgressVO> recent();
}
