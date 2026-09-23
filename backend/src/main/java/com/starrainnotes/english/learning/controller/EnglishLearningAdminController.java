package com.starrainnotes.english.learning.controller;

import com.starrainnotes.english.learning.dto.AdminLearningAnalyticsView;
import com.starrainnotes.english.learning.application.EnglishLearningAnalyticsQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/english/analytics")
public class EnglishLearningAdminController {
    private final EnglishLearningAnalyticsQueryService service;

    public EnglishLearningAdminController(EnglishLearningAnalyticsQueryService service) {
        this.service = service;
    }

    @GetMapping
    public AdminLearningAnalyticsView analytics(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "ALL") String type) {
        return service.analytics(days, type);
    }
}
