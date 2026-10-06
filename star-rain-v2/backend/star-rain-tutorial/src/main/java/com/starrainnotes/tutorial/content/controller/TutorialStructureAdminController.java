package com.starrainnotes.tutorial.content.controller;

import com.starrainnotes.common.result.ApiResponse;
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
    private final TutorialContentService contentService;

    @PatchMapping("/tutorial-groups/{groupId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<TutorialGroupVO> updateGroup(@PathVariable Long groupId,
                                                     @Valid @RequestBody GroupNameDTO request) {
        return ApiResponse.ok(contentService.updateGroup(groupId, request.getTitle()));
    }

    @DeleteMapping("/tutorial-groups/{groupId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Void> deleteGroup(@PathVariable Long groupId) {
        contentService.deleteGroup(groupId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/tutorial-groups/{groupId}/chapters")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<TutorialChapterVO> createChapter(@PathVariable Long groupId,
                                                         @Valid @RequestBody ChapterCreateDTO request) {
        return ApiResponse.ok(contentService.createChapter(groupId, request));
    }

    @PutMapping("/tutorial-groups/{groupId}/chapters/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Void> reorderChapters(@PathVariable Long groupId,
                                              @Valid @RequestBody IdOrderDTO request) {
        contentService.reorderChapters(groupId, request.getIds());
        return ApiResponse.ok(null);
    }

    @GetMapping("/tutorial-chapters/{chapterId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<TutorialChapterVO> chapter(@PathVariable Long chapterId) {
        return ApiResponse.ok(contentService.chapter(chapterId));
    }

    @PatchMapping("/tutorial-chapters/{chapterId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<TutorialChapterVO> updateChapter(@PathVariable Long chapterId,
                                                         @Valid @RequestBody ChapterUpdateDTO request) {
        return ApiResponse.ok(contentService.updateChapter(chapterId, request));
    }

    @PutMapping("/tutorial-chapters/{chapterId}/body")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<TutorialChapterVO> updateBody(@PathVariable Long chapterId,
                                                      @Valid @RequestBody ChapterBodyDTO request) {
        return ApiResponse.ok(contentService.updateChapterBody(chapterId, request));
    }

    @DeleteMapping("/tutorial-chapters/{chapterId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Void> deleteChapter(@PathVariable Long chapterId) {
        contentService.deleteChapter(chapterId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/tutorial-chapters/{chapterId}/publish")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Void> publishChapter(@PathVariable Long chapterId) {
        contentService.publishChapter(chapterId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/tutorial-chapters/{chapterId}/withdraw")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Void> withdrawChapter(@PathVariable Long chapterId) {
        contentService.withdrawChapter(chapterId);
        return ApiResponse.ok(null);
    }

}
