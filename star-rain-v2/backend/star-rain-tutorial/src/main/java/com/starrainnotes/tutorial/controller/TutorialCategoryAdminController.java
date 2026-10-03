package com.starrainnotes.tutorial.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.constant.TutorialPermissions;
import com.starrainnotes.tutorial.dto.CategoryNameDTO;
import com.starrainnotes.tutorial.dto.IdOrderDTO;
import com.starrainnotes.tutorial.service.TutorialContentService;
import com.starrainnotes.tutorial.vo.TutorialCategoryVO;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tutorial-categories")
public class TutorialCategoryAdminController {
    private final TutorialContentService contentService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<List<TutorialCategoryVO>> categories() {
        return ApiResponse.ok(contentService.categories());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + TutorialPermissions.PUBLISH + "')")
    public ApiResponse<TutorialCategoryVO> create(@Valid @RequestBody CategoryNameDTO request) {
        return ApiResponse.ok(contentService.createCategory(request.getName()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.PUBLISH + "')")
    public ApiResponse<TutorialCategoryVO> update(@PathVariable Long id,
                                                   @Valid @RequestBody CategoryNameDTO request) {
        return ApiResponse.ok(contentService.updateCategory(id, request.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.PUBLISH + "')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contentService.deleteCategory(id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.PUBLISH + "')")
    public ApiResponse<Void> reorder(@Valid @RequestBody IdOrderDTO request) {
        contentService.reorderCategories(request.getIds());
        return ApiResponse.ok(null);
    }

    @PutMapping("/{categoryId}/tutorials/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Void> reorderTutorials(@PathVariable Long categoryId,
                                               @Valid @RequestBody IdOrderDTO request) {
        contentService.reorderTutorials(categoryId, request.getIds());
        return ApiResponse.ok(null);
    }
}
