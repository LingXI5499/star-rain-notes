package com.starrainnotes.site.controller;

import com.starrainnotes.site.dto.SiteDashboardView;
import com.starrainnotes.site.service.DashboardAggregationService;

import com.starrainnotes.common.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/site")
@PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('site:dashboard-read')")
public class SiteDashboardController {
    private final DashboardAggregationService service;

    @GetMapping("/dashboard")
    public ApiResponse<SiteDashboardView> dashboard() { return ApiResponse.ok(service.dashboard()); }
}
