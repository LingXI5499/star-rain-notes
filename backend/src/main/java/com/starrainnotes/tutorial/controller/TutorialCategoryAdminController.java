package com.starrainnotes.tutorial.controller;

import com.starrainnotes.tutorial.dto.CategoryNodeView;
import com.starrainnotes.tutorial.dto.CreateCategoryRequest;
import com.starrainnotes.tutorial.dto.MoveCategoryRequest;
import com.starrainnotes.tutorial.dto.UpdateCategoryRequest;
import com.starrainnotes.tutorial.service.TutorialCategoryCommandService;
import com.starrainnotes.tutorial.service.TutorialCategoryQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin tutorial category management (04 §10).
 */
@RestController
@RequestMapping("/api/v1/admin/tutorial-categories")
@RequiredArgsConstructor
public class TutorialCategoryAdminController {
    private final TutorialCategoryCommandService commandService;
    private final TutorialCategoryQueryService queryService;

    @GetMapping("/tree")
    public List<CategoryNodeView> tree() {
        return queryService.adminTree();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryNodeView create(@Valid @RequestBody CreateCategoryRequest request) {
        return commandService.create(request);
    }

    @PutMapping("/{categoryId}")
    public CategoryNodeView update(@PathVariable Long categoryId,
                                   @Valid @RequestBody UpdateCategoryRequest request) {
        return commandService.update(categoryId, request);
    }

    @PostMapping("/{categoryId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void move(@PathVariable Long categoryId,
                     @Valid @RequestBody MoveCategoryRequest request) {
        commandService.move(categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long categoryId) {
        commandService.delete(categoryId);
    }
}
