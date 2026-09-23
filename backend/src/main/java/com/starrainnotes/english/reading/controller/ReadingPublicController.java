package com.starrainnotes.english.reading.controller;

import com.starrainnotes.english.reading.dto.ReadingArticleView;
import com.starrainnotes.english.shared.exercise.dto.CheckAnswerRequest;
import com.starrainnotes.english.shared.exercise.dto.CheckResultView;
import com.starrainnotes.english.shared.exercise.dto.ExercisePublicView;
import com.starrainnotes.english.reading.dto.ReadingHomeView;
import com.starrainnotes.english.reading.dto.ReadingPageView;
import com.starrainnotes.english.reading.application.ReadingQueryService;
import com.starrainnotes.english.reading.application.ReadingExerciseApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public reading entry (方案 §10.3, §十). Only PUBLISHED articles are served;
 * answer configs are sanitized and scoring happens server-side.
 */
@RestController
@RequestMapping("/api/v1/public/english/reading")
public class ReadingPublicController {

    private final ReadingQueryService articleService;
    private final ReadingExerciseApplicationService exerciseService;

    public ReadingPublicController(ReadingQueryService articleService,
                                   ReadingExerciseApplicationService exerciseService) {
        this.articleService = articleService;
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public ReadingHomeView home() {
        return articleService.home();
    }

    @GetMapping("/articles")
    public ReadingPageView list(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int pageSize,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) Integer level,
                                @RequestParam(required = false) String cefr,
                                @RequestParam(required = false) Long topic,
                                @RequestParam(required = false) Long genre) {
        return articleService.publicList(page, pageSize, q, level, cefr, topic, genre);
    }

    @GetMapping("/articles/{slug}")
    public ReadingArticleView detail(@PathVariable String slug) {
        return articleService.publicGet(slug);
    }

    @GetMapping("/articles/{slug}/exercises")
    public List<ExercisePublicView> exercises(@PathVariable String slug) {
        ReadingArticleView article = articleService.publicGet(slug);
        return exerciseService.publicListPublished(article.id());
    }

    @PostMapping("/articles/{slug}/check")
    public CheckResultView check(@PathVariable String slug,
                                        @Valid @RequestBody CheckAnswerRequest request) {
        ReadingArticleView article = articleService.publicGet(slug);
        return exerciseService.check(article.id(), request);
    }
}
