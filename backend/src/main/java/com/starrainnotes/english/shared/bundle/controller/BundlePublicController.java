package com.starrainnotes.english.shared.bundle.controller;

import com.starrainnotes.english.shared.bundle.dto.BundleView;
import com.starrainnotes.english.shared.bundle.service.LearningBundleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public learning-bundle entry (方案 §10.6). Only PUBLISHED bundles are served.
 */
@RestController
@RequestMapping("/api/v1/public/english/bundles")
public class BundlePublicController {

    private final LearningBundleService service;

    public BundlePublicController(LearningBundleService service) {
        this.service = service;
    }

    @GetMapping("/{slug}")
    public BundleView get(@PathVariable String slug) {
        return service.publicGet(slug);
    }
}
