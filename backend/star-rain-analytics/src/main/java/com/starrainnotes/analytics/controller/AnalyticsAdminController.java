package com.starrainnotes.analytics.controller;

import com.starrainnotes.analytics.service.AnalyticsService;
import com.starrainnotes.common.result.ApiResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('analytics:read')")
public class AnalyticsAdminController {
    private final AnalyticsService service;

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() { return ApiResponse.ok(service.siteSummary()); }

    @GetMapping("/trend")
    public ApiResponse<List<Map<String, Object>>> trend(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(service.trend(start(startDate), end(endDate)));
    }

    @GetMapping("/hot-content")
    public ApiResponse<List<Map<String, Object>>> hot(
        @RequestParam(required = false) String type,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(service.hotContent(type, start(startDate), end(endDate), limit));
    }

    @GetMapping("/referrers")
    public ApiResponse<List<Map<String, Object>>> referrers(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(service.referrers(start(startDate), end(endDate)));
    }

    private LocalDate end(LocalDate value) { return value == null ? LocalDate.now(ZoneOffset.UTC) : value; }
    private LocalDate start(LocalDate value) { return value == null ? LocalDate.now(ZoneOffset.UTC).minusDays(29) : value; }
}
