package com.starrainnotes.tutorial.content.controller;

import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.content.service.TutorialOperationService;
import com.starrainnotes.tutorial.content.constant.TutorialPermissions;
import com.starrainnotes.tutorial.content.dto.ChapterBodyDTO;
import com.starrainnotes.tutorial.content.dto.ChapterCreateDTO;
import com.starrainnotes.tutorial.content.dto.ChapterUpdateDTO;
import com.starrainnotes.tutorial.content.dto.GroupNameDTO;
import com.starrainnotes.tutorial.content.dto.IdOrderDTO;
import com.starrainnotes.tutorial.content.service.TutorialContentService;
import com.starrainnotes.tutorial.content.vo.TutorialChapterVO;
import com.starrainnotes.tutorial.content.vo.TutorialGroupVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class TutorialStructureAdminController {
    private final TutorialOperationService operations;
    private final TutorialContentService contentService;
    private final TutorialPublicationService publicationService;

    @PatchMapping("/tutorial-groups/{groupId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> updateGroup(@PathVariable Long groupId,
                                                     @Valid @RequestBody GroupNameDTO request) {
        return ApiResponse.ok(operations.request("updateGroup", new Object[]{groupId, request.getTitle()}));
    }

    @DeleteMapping("/tutorial-groups/{groupId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> deleteGroup(@PathVariable Long groupId) {
        return ApiResponse.ok(operations.request("deleteGroup", new Object[]{groupId}));
    }

    @PostMapping("/tutorial-groups/{groupId}/chapters")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> createChapter(@PathVariable Long groupId,
                                                         @Valid @RequestBody ChapterCreateDTO request) {
        return ApiResponse.ok(operations.request("createChapter", new Object[]{groupId, request}));
    }

    @PutMapping("/tutorial-groups/{groupId}/chapters/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> reorderChapters(@PathVariable Long groupId,
                                              @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(operations.request("reorderChapters", new Object[]{groupId, request.getIds()}));
    }

    @GetMapping("/tutorial-chapters/{chapterId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<TutorialChapterVO> chapter(@PathVariable Long chapterId) {
        return ApiResponse.ok(contentService.chapter(chapterId));
    }

    @PatchMapping("/tutorial-chapters/{chapterId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> updateChapter(@PathVariable Long chapterId,
                                                         @Valid @RequestBody ChapterUpdateDTO request) {
        return ApiResponse.ok(operations.request("updateChapter", new Object[]{chapterId, request}));
    }

    @PutMapping("/tutorial-chapters/{chapterId}/body")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> updateBody(@PathVariable Long chapterId,
                                                      @Valid @RequestBody ChapterBodyDTO request) {
        return ApiResponse.ok(operations.request("updateChapterBody", new Object[]{chapterId, request}));
    }

    @DeleteMapping("/tutorial-chapters/{chapterId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> deleteChapter(@PathVariable Long chapterId) {
        return ApiResponse.ok(operations.request("deleteChapter", new Object[]{chapterId}));
    }

    @PostMapping("/tutorial-chapters/{chapterId}/publish")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> publishChapter(@PathVariable Long chapterId) {
        return ApiResponse.ok(operations.request("publishChapter", new Object[]{chapterId}));
    }

    @PostMapping("/tutorial-chapters/{chapterId}/withdraw")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> withdrawChapter(@PathVariable Long chapterId) {
        return ApiResponse.ok(operations.request("withdrawChapter", new Object[]{chapterId}));
    }

}
