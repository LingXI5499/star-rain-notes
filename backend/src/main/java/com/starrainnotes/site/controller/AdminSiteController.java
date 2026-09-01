package com.starrainnotes.site.controller;

import com.starrainnotes.site.dto.AdminSiteSettingsView;
import com.starrainnotes.site.dto.DashboardView;
import com.starrainnotes.site.dto.UpdateSiteSettingsRequest;
import com.starrainnotes.site.service.DashboardService;
import com.starrainnotes.site.service.SiteService;
import jakarta.validation.Valid;
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
public class AdminSiteController {

    private final SiteService siteService;
    private final DashboardService dashboardService;

    public AdminSiteController(SiteService siteService, DashboardService dashboardService) {
        this.siteService = siteService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardView dashboard() {
        return dashboardService.getDashboard();
    }

    @GetMapping("/site-settings")
    public AdminSiteSettingsView getSiteSettings() {
        return siteService.getAdminSettings();
    }

    @PutMapping("/site-settings")
    public AdminSiteSettingsView updateSiteSettings(@Valid @RequestBody UpdateSiteSettingsRequest request) {
        return siteService.updateAdminSettings(request);
    }
}
