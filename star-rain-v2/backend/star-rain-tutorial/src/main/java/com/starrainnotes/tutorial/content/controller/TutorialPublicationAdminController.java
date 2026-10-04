package com.starrainnotes.tutorial.content.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.tutorial.content.constant.TutorialPermissions;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import com.starrainnotes.tutorial.content.vo.TutorialAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tutorials/{id}")
public class TutorialPublicationAdminController {
    private final TutorialPublicationService publicationService;

    @GetMapping("/preview")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<JsonNode> preview(@PathVariable Long id) {
        return ApiResponse.ok(publicationService.previewTutorial(id));
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.PUBLISH + "')")
    public ApiResponse<TutorialAdminVO> publish(@PathVariable Long id) {
        return ApiResponse.ok(publicationService.publish(id));
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.WITHDRAW + "')")
    public ApiResponse<TutorialAdminVO> withdraw(@PathVariable Long id) {
        return ApiResponse.ok(publicationService.withdraw(id));
    }

    @PostMapping("/restore-publication")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.WITHDRAW + "')")
    public ApiResponse<TutorialAdminVO> restore(@PathVariable Long id) {
        return ApiResponse.ok(publicationService.restore(id));
    }

    @PostMapping("/submit-review")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.SUBMIT + "')")
    public ApiResponse<TutorialAdminVO> submitReview(@PathVariable Long id) {
        return ApiResponse.ok(publicationService.submitReview(id));
    }
}
