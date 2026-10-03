package com.starrainnotes.tutorial.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.constant.TutorialPermissions;
import com.starrainnotes.tutorial.service.TutorialPublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tutorial-chapters")
public class TutorialChapterPreviewAdminController {
    private final TutorialPublicationService publicationService;

    @GetMapping("/{chapterId}/preview")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<JsonNode> preview(@PathVariable Long chapterId) {
        return ApiResponse.ok(publicationService.previewChapter(chapterId));
    }
}
