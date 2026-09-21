package com.starrainnotes.profile.controller;

import com.starrainnotes.profile.dto.AdminAboutView;
import com.starrainnotes.profile.dto.UpdateAboutRequest;
import com.starrainnotes.profile.dto.UpdateSelectedContentRequest;
import com.starrainnotes.profile.service.ProfileCommandService;
import com.starrainnotes.profile.service.ProfileQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AboutAdminController {

    private final ProfileCommandService commandService;
    private final ProfileQueryService queryService;

    @GetMapping
    public AdminAboutView get() {
        return queryService.getAdmin();
    }

    @PutMapping
    public AdminAboutView update(@Valid @RequestBody UpdateAboutRequest request) {
        return commandService.update(request);
    }

    @PutMapping("/selected-content")
    public AdminAboutView updateSelectedContent(@Valid @RequestBody UpdateSelectedContentRequest request) {
        return commandService.updateSelectedContent(request);
    }
}
