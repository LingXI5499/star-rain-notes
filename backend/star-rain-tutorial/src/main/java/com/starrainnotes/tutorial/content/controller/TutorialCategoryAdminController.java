package com.starrainnotes.tutorial.content.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.content.service.TutorialOperationService;
import com.starrainnotes.tutorial.content.constant.TutorialPermissions;
import com.starrainnotes.tutorial.content.dto.CategoryNameDTO;
import com.starrainnotes.tutorial.content.dto.IdOrderDTO;
import com.starrainnotes.tutorial.content.service.TutorialContentService;
import com.starrainnotes.tutorial.content.vo.TutorialCategoryVO;
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
    private final TutorialOperationService operations;
    private final TutorialContentService contentService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<List<TutorialCategoryVO>> categories() {
        return ApiResponse.ok(contentService.categories());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> create(@Valid @RequestBody CategoryNameDTO request) {
        return ApiResponse.ok(operations.request("createCategory", new Object[]{request.getName()}));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> update(@PathVariable Long id,
                                                   @Valid @RequestBody CategoryNameDTO request) {
        return ApiResponse.ok(operations.request("updateCategory", new Object[]{id, request.getName()}));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> delete(@PathVariable Long id) {
        return ApiResponse.ok(operations.request("deleteCategory", new Object[]{id}));
    }

    @PutMapping("/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> reorder(@Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(operations.request("reorderCategories", new Object[]{request.getIds()}));
    }

    @PutMapping("/{categoryId}/tutorials/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> reorderTutorials(@PathVariable Long categoryId,
                                               @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(operations.request("reorderTutorials", new Object[]{categoryId, request.getIds()}));
    }
}
