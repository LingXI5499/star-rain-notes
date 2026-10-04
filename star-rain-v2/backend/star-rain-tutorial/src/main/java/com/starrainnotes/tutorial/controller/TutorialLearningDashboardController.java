package com.starrainnotes.tutorial.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.service.LearningDashboardService;
import com.starrainnotes.tutorial.vo.LearningHistoryVO;
import com.starrainnotes.tutorial.vo.LearningStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account/learning")
@PreAuthorize("isAuthenticated()")
public class TutorialLearningDashboardController {
    private final LearningDashboardService dashboard;

    @GetMapping("/history")
    public ApiResponse<PageResult<LearningHistoryVO>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(dashboard.history(page, pageSize));
    }

    @GetMapping("/statistics")
    public ApiResponse<LearningStatisticsVO> statistics() {
        return ApiResponse.ok(dashboard.statistics());
    }
}
