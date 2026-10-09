package com.starrainnotes.tutorial.content.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import com.starrainnotes.tutorial.content.service.TutorialPublicReadService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class TutorialPublicController {
    private final TutorialPublicationService publicationService;
    private final TutorialPublicReadService reading;

    @GetMapping("/tutorial-categories/tree")
    public ApiResponse<List<JsonNode>> categories() {
        return ApiResponse.ok(publicationService.publicCategories());
    }

    @GetMapping("/tutorials")
    public ApiResponse<PageResult<JsonNode>> tutorials(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(publicationService.publicTutorials(categoryId, search, page, pageSize));
    }

    @GetMapping("/tutorials/{slug}")
    public ApiResponse<JsonNode> tutorial(@PathVariable String slug) {
        return ApiResponse.ok(reading.tutorial(slug));
    }

    @GetMapping("/tutorials/{slug}/chapters/{chapterSlug}")
    public ApiResponse<JsonNode> chapter(@PathVariable String slug, @PathVariable String chapterSlug) {
        return ApiResponse.ok(reading.chapter(slug, chapterSlug));
    }

}
