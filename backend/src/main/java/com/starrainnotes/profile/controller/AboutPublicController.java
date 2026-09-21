package com.starrainnotes.profile.controller;

import com.starrainnotes.profile.dto.PublicAboutView;
import com.starrainnotes.profile.service.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public About (04 §14).
 */
@RestController
@RequestMapping("/api/v1/public/about")
@RequiredArgsConstructor
public class AboutPublicController {

    private final ProfileQueryService queryService;

    @GetMapping
    public PublicAboutView get() {
        return queryService.getPublic();
    }
}
