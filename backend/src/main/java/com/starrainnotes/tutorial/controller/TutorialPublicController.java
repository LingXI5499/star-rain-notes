package com.starrainnotes.tutorial.controller;

import com.starrainnotes.tutorial.dto.PublicCategoryNodeView;
import com.starrainnotes.tutorial.dto.PublicTutorialDetailView;
import com.starrainnotes.tutorial.dto.PublicTutorialSummaryView;
import com.starrainnotes.tutorial.service.TutorialCategoryQueryService;
import com.starrainnotes.tutorial.service.TutorialQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public tutorial endpoints (04 §10). Draft/Withdrawn content returns 404.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class TutorialPublicController {

    private final TutorialCategoryQueryService categoryQueryService;
    private final TutorialQueryService queryService;

    @GetMapping("/tutorial-categories/tree")
    public List<PublicCategoryNodeView> categoryTree() {
        return categoryQueryService.publicTree();
    }

    @GetMapping("/tutorials")
    public List<PublicTutorialSummaryView> tutorials(
            @RequestParam(required = false) String categorySlug) {
        return queryService.publicList(categorySlug);
    }

    @GetMapping("/tutorials/{tutorialSlug}")
    public PublicTutorialDetailView tutorialDetail(@PathVariable String tutorialSlug) {
        return queryService.publicDetail(tutorialSlug);
    }

    @GetMapping("/tutorials/{tutorialSlug}/chapters/{chapterSlug}")
    public com.starrainnotes.tutorial.dto.PublicChapterView chapterDetail(@PathVariable String tutorialSlug,
                                                                          @PathVariable String chapterSlug) {
        return queryService.publicChapter(tutorialSlug, chapterSlug);
    }
}
