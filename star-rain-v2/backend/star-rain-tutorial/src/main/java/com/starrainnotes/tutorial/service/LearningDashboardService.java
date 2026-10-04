package com.starrainnotes.tutorial.service;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.vo.LearningHistoryVO;
import com.starrainnotes.tutorial.vo.LearningStatisticsVO;

public interface LearningDashboardService {
    PageResult<LearningHistoryVO> history(int page, int pageSize);
    LearningStatisticsVO statistics();
}
