package com.starrainnotes.tutorial.content.controller;

import org.springframework.web.bind.annotation.RequestParam;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.content.service.TutorialOperationService;
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
    private final TutorialOperationService operations;
    private final TutorialPublicationService publicationService;

    @PostMapping("/submit-review")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.SUBMIT + "')")
    public ApiResponse<Object> submit(@PathVariable Long id,
            @RequestParam(required = false) Long chapterId) {
        return ApiResponse.ok(operations.request(chapterId == null ? "publish" : "publishChapter",
                new Object[]{chapterId == null ? id : chapterId}));
    }

    @GetMapping("/preview")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<JsonNode> preview(@PathVariable Long id) {
        return ApiResponse.ok(publicationService.previewTutorial(id));
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> publish(@PathVariable Long id) {
        return ApiResponse.ok(operations.request("publish", new Object[]{id}));
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> withdraw(@PathVariable Long id) {
        return ApiResponse.ok(operations.request("withdraw", new Object[]{id}));
    }

}
