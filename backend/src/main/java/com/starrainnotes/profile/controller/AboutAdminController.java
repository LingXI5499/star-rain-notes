package com.starrainnotes.profile.controller;

import com.starrainnotes.profile.dto.AdminAboutView;
import com.starrainnotes.profile.dto.UpdateAboutRequest;
import com.starrainnotes.profile.dto.UpdateSelectedContentRequest;
import com.starrainnotes.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin About management (04 §14).
 */
@RestController
@RequestMapping("/api/v1/admin/about")
public class AboutAdminController {

    private final ProfileService profileService;

    public AboutAdminController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public AdminAboutView get() {
        return profileService.getAdmin();
    }

    @PutMapping
    public AdminAboutView update(@Valid @RequestBody UpdateAboutRequest request) {
        return profileService.update(request);
    }

    @PutMapping("/selected-content")
    public AdminAboutView updateSelectedContent(@Valid @RequestBody UpdateSelectedContentRequest request) {
        return profileService.updateSelectedContent(request);
    }
}
