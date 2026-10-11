package com.starrainnotes.tutorial.learning.service;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.learning.vo.LearningHistoryVO;
import com.starrainnotes.tutorial.learning.vo.LearningStatisticsVO;

public interface LearningDashboardService {
    PageResult<LearningHistoryVO> history(int page, int pageSize);
    LearningStatisticsVO statistics();
}
