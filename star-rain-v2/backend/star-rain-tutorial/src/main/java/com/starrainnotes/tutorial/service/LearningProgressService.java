package com.starrainnotes.tutorial.service;

import com.starrainnotes.tutorial.dto.LearningProgressDTO;
import com.starrainnotes.tutorial.vo.LearningProgressVO;
import com.starrainnotes.tutorial.vo.LearningTutorialProgressVO;
import java.util.List;

public interface LearningProgressService {
    LearningProgressVO save(Long chapterId, LearningProgressDTO request);
    LearningProgressVO complete(Long chapterId);
    LearningProgressVO chapter(Long chapterId);
    LearningTutorialProgressVO tutorial(Long tutorialId);
    List<LearningProgressVO> recent();
}
