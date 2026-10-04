package com.starrainnotes.tutorial.content.controller;

import com.starrainnotes.analytics.api.AnalyticsRecordApi;
import com.starrainnotes.analytics.api.ContentViewEvent;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired(required = false)
    private AnalyticsRecordApi analytics;

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
        JsonNode detail = publicationService.publicTutorial(slug);
        if (analytics != null) analytics.recordContentView(new ContentViewEvent("TUTORIAL", detail.path("id").asLong(), "/tutorials/:slug"));
        return ApiResponse.ok(detail);
    }

    @GetMapping("/tutorials/{slug}/chapters/{chapterSlug}")
    public ApiResponse<JsonNode> chapter(@PathVariable String slug, @PathVariable String chapterSlug) {
        JsonNode detail = publicationService.publicChapter(slug, chapterSlug);
        if (analytics != null) analytics.recordContentView(new ContentViewEvent("CHAPTER", detail.path("id").asLong(), "/tutorials/:slug/:chapterSlug"));
        return ApiResponse.ok(detail);
    }

    @GetMapping("/tutorials/{slug}/chapters/{chapterSlug}/questions/{questionId}/answer")
    public ApiResponse<JsonNode> answer(@PathVariable String slug, @PathVariable String chapterSlug,
                                        @PathVariable String questionId) {
        return ApiResponse.ok(publicationService.publicQuestionAnswer(slug, chapterSlug, questionId));
    }
}
