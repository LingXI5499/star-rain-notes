package com.starrainnotes.english.writing.controller;

import com.starrainnotes.english.shared.exercise.dto.CheckAnswerRequest;
import com.starrainnotes.english.shared.exercise.dto.CheckResultView;
import com.starrainnotes.english.shared.exercise.dto.ExercisePublicView;
import com.starrainnotes.english.writing.application.WritingExerciseApplicationService;
import com.starrainnotes.english.writing.application.WritingPromptQueryService;
import com.starrainnotes.english.writing.application.WritingResourceQueryService;
import com.starrainnotes.english.writing.dto.WritingPageView;
import com.starrainnotes.english.writing.dto.WritingPromptSummaryView;
import com.starrainnotes.english.writing.dto.WritingPromptView;
import com.starrainnotes.english.writing.dto.WritingResourceSummaryView;
import com.starrainnotes.english.writing.dto.WritingResourceView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/english/writing")
public class WritingPublicController {
    private final WritingResourceQueryService resources;
    private final WritingPromptQueryService prompts;
    private final WritingExerciseApplicationService exercises;

    public WritingPublicController(WritingResourceQueryService resources, WritingPromptQueryService prompts,
                                   WritingExerciseApplicationService exercises) {
        this.resources = resources;
        this.prompts = prompts;
        this.exercises = exercises;
    }

    @GetMapping("/resources")
    public WritingPageView<WritingResourceSummaryView> resources(@RequestParam(defaultValue = "1") int page,
                                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                                 @RequestParam(required = false) String q,
                                                                 @RequestParam(required = false) String kind,
                                                                 @RequestParam(required = false) String expressionLevel,
                                                                 @RequestParam(required = false) String cefr,
                                                                 @RequestParam(required = false) Long topic,
                                                                 @RequestParam(required = false) Long genre) {
        return resources.publicList(page, pageSize, q, kind, expressionLevel, cefr, topic, genre);
    }

    @GetMapping("/resources/{slug}")
    public WritingResourceView resource(@PathVariable String slug) {
        return resources.publicGet(slug);
    }

    @GetMapping("/prompts")
    public WritingPageView<WritingPromptSummaryView> prompts(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int pageSize,
                                                             @RequestParam(required = false) String q,
                                                             @RequestParam(required = false) String cefr,
                                                             @RequestParam(required = false) Long topic,
                                                             @RequestParam(required = false) Long genre) {
        return prompts.publicList(page, pageSize, q, cefr, topic, genre);
    }

    @GetMapping("/prompts/{slug}")
    public WritingPromptView prompt(@PathVariable String slug) {
        return prompts.publicGet(slug);
    }

    @GetMapping("/prompts/{slug}/exercises")
    public List<ExercisePublicView> exercises(@PathVariable String slug) {
        return exercises.publicList(slug);
    }

    @PostMapping("/prompts/{slug}/check")
    public CheckResultView check(@PathVariable String slug, @Valid @RequestBody CheckAnswerRequest request) {
        return exercises.check(slug, request);
    }
}
