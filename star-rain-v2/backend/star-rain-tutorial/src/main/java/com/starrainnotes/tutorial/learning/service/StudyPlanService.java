package com.starrainnotes.tutorial.learning.service;

import com.starrainnotes.tutorial.learning.dto.StudyPlanDTO;
import com.starrainnotes.tutorial.learning.vo.StudyPlanVO;
import com.starrainnotes.tutorial.learning.vo.StudyTaskVO;
import java.util.List;

public interface StudyPlanService {
    StudyPlanVO create(StudyPlanDTO request);
    StudyPlanVO update(Long planId, StudyPlanDTO request);
    StudyPlanVO transition(Long planId, String action);
    StudyPlanVO plan(Long planId);
    List<StudyPlanVO> plans();
    List<StudyTaskVO> planTasks(Long planId);
    List<StudyTaskVO> today();
    StudyTaskVO start(Long taskId);
    StudyTaskVO complete(Long taskId);
    StudyTaskVO skip(Long taskId);
}
