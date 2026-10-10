package com.starrainnotes.tutorial.content.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.content.service.TutorialOperationService;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.constant.TutorialPermissions;
import com.starrainnotes.tutorial.content.dto.GroupNameDTO;
import com.starrainnotes.tutorial.content.dto.IdOrderDTO;
import com.starrainnotes.tutorial.content.dto.TutorialCreateDTO;
import com.starrainnotes.tutorial.content.dto.TutorialUpdateDTO;
import com.starrainnotes.tutorial.content.service.TutorialContentService;
import com.starrainnotes.tutorial.content.vo.TutorialAdminVO;
import com.starrainnotes.tutorial.content.vo.TutorialCurriculumVO;
import com.starrainnotes.tutorial.content.vo.TutorialGroupVO;
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
    private final TutorialOperationService operations;
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
    public ApiResponse<Object> create(@Valid @RequestBody TutorialCreateDTO request) {
        return ApiResponse.ok(operations.request("createTutorial", new Object[]{request}));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<TutorialAdminVO> tutorial(@PathVariable Long id) {
        return ApiResponse.ok(contentService.tutorial(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> update(@PathVariable Long id,
                                                @Valid @RequestBody TutorialUpdateDTO request) {
        return ApiResponse.ok(operations.request("updateTutorial", new Object[]{id, request}));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> delete(@PathVariable Long id) {
        return ApiResponse.ok(operations.request("deleteTutorial", new Object[]{id}));
    }

    @GetMapping("/{id}/curriculum")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<TutorialCurriculumVO> curriculum(@PathVariable Long id) {
        return ApiResponse.ok(contentService.curriculum(id));
    }

    @PostMapping("/{id}/groups")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> createGroup(@PathVariable Long id,
                                                     @Valid @RequestBody GroupNameDTO request) {
        return ApiResponse.ok(operations.request("createGroup", new Object[]{id, request.getTitle()}));
    }

    @PutMapping("/{id}/groups/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> reorderGroups(@PathVariable Long id,
                                            @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(operations.request("reorderGroups", new Object[]{id, request.getIds()}));
    }
}
