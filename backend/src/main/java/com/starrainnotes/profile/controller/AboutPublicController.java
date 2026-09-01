package com.starrainnotes.profile.controller;

import com.starrainnotes.profile.dto.PublicAboutView;
import com.starrainnotes.profile.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public About (04 §14).
 */
@RestController
@RequestMapping("/api/v1/public/about")
public class AboutPublicController {

    private final ProfileService profileService;

    public AboutPublicController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public PublicAboutView get() {
        return profileService.getPublic();
    }
}
