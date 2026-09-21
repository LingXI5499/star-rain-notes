package com.starrainnotes.site.controller;

import com.starrainnotes.site.dto.AdminSiteSettingsView;
import com.starrainnotes.site.dto.DashboardView;
import com.starrainnotes.site.dto.UpdateSiteSettingsRequest;
import com.starrainnotes.site.service.DashboardQueryService;
import com.starrainnotes.site.service.SiteCommandService;
import com.starrainnotes.site.service.SiteQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin site foundation (04 §9): dashboard and singleton site settings.
 * All endpoints require an authenticated admin session (filter chain).
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminSiteController {

    private final SiteCommandService commandService;
    private final SiteQueryService queryService;
    private final DashboardQueryService dashboardService;

    @GetMapping("/dashboard")
    public DashboardView dashboard() {
        return dashboardService.getDashboard();
    }

    @GetMapping("/site-settings")
    public AdminSiteSettingsView getSiteSettings() {
        return queryService.getAdminSettings();
    }

    @PutMapping("/site-settings")
    public AdminSiteSettingsView updateSiteSettings(@Valid @RequestBody UpdateSiteSettingsRequest request) {
        return commandService.updateAdminSettings(request);
    }
}
