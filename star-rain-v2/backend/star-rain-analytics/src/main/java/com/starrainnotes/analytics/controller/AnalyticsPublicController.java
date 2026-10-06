package com.starrainnotes.analytics.controller;

import com.starrainnotes.analytics.dto.PageViewDTO;
import com.starrainnotes.analytics.exception.AnalyticsRouteInvalidException;
import com.starrainnotes.analytics.security.AnalyticsSubmissionLimiter;
import com.starrainnotes.analytics.service.AnalyticsService;
import com.starrainnotes.common.result.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/analytics")
public class AnalyticsPublicController {
    private final AnalyticsService service;
    private final AnalyticsSubmissionLimiter limiter;

    @PostMapping("/page-view")
    public ApiResponse<Void> pageView(@RequestBody PageViewDTO request, HttpServletRequest http) {
        if (request == null) throw new AnalyticsRouteInvalidException();
        limiter.check(http.getRemoteAddr());
        service.recordPageView(request.getRouteKey(), request.getReferrer());
        return ApiResponse.ok(null);
    }
}
