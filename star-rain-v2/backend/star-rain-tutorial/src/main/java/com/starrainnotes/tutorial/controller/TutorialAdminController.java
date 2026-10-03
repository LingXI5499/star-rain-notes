package com.starrainnotes.tutorial.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.constant.TutorialPermissions;
import com.starrainnotes.tutorial.dto.GroupNameDTO;
import com.starrainnotes.tutorial.dto.IdOrderDTO;
import com.starrainnotes.tutorial.dto.TutorialCreateDTO;
import com.starrainnotes.tutorial.dto.TutorialUpdateDTO;
import com.starrainnotes.tutorial.service.TutorialContentService;
import com.starrainnotes.tutorial.vo.TutorialAdminVO;
import com.starrainnotes.tutorial.vo.TutorialCurriculumVO;
import com.starrainnotes.tutorial.vo.TutorialGroupVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tutorials")
public class TutorialAdminController {
    private final TutorialContentService contentService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<PageResult<TutorialAdminVO>> tutorials(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(contentService.tutorials(page, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<TutorialAdminVO> create(@Valid @RequestBody TutorialCreateDTO request) {
        return ApiResponse.ok(contentService.createTutorial(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<TutorialAdminVO> tutorial(@PathVariable Long id) {
        return ApiResponse.ok(contentService.tutorial(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<TutorialAdminVO> update(@PathVariable Long id,
                                                @Valid @RequestBody TutorialUpdateDTO request) {
        return ApiResponse.ok(contentService.updateTutorial(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.PUBLISH + "')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contentService.deleteTutorial(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/curriculum")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<TutorialCurriculumVO> curriculum(@PathVariable Long id) {
        return ApiResponse.ok(contentService.curriculum(id));
    }

    @PostMapping("/{id}/groups")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<TutorialGroupVO> createGroup(@PathVariable Long id,
                                                     @Valid @RequestBody GroupNameDTO request) {
        return ApiResponse.ok(contentService.createGroup(id, request.getTitle()));
    }

    @PutMapping("/{id}/groups/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Void> reorderGroups(@PathVariable Long id,
                                            @Valid @RequestBody IdOrderDTO request) {
        contentService.reorderGroups(id, request.getIds());
        return ApiResponse.ok(null);
    }
}
