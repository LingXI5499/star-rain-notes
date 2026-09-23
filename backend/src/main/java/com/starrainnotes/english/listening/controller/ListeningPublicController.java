package com.starrainnotes.english.listening.controller;

import com.starrainnotes.english.listening.dto.ListeningHomeView;
import com.starrainnotes.english.listening.dto.ListeningItemView;
import com.starrainnotes.english.listening.dto.ListeningPageView;
import com.starrainnotes.english.listening.dto.ListeningSegmentView;
import com.starrainnotes.english.listening.dto.PronunciationRuleView;
import com.starrainnotes.english.listening.application.ListeningQueryService;
import com.starrainnotes.english.listening.application.ListeningSegmentService;
import com.starrainnotes.english.listening.application.PronunciationRuleQueryService;
import com.starrainnotes.english.listening.application.ListeningExerciseApplicationService;
import com.starrainnotes.english.shared.exercise.dto.CheckAnswerRequest;
import com.starrainnotes.english.shared.exercise.dto.CheckResultView;
import com.starrainnotes.english.shared.exercise.dto.ExercisePublicView;
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
 * Public listening entry (阶段三 §五). Only PUBLISHED material is served; DRAFT
 * / WITHDRAWN return 404 and answer configs are sanitized.
 */
@RestController
@RequestMapping("/api/v1/public/english/listening")
public class ListeningPublicController {

    private final ListeningQueryService queries;
    private final ListeningSegmentService segments;
    private final PronunciationRuleQueryService pronunciationQueries;
    private final ListeningExerciseApplicationService exerciseService;

    public ListeningPublicController(ListeningQueryService queries, ListeningSegmentService segments,
            PronunciationRuleQueryService pronunciationQueries,
            ListeningExerciseApplicationService exerciseService) {
        this.queries = queries;
        this.segments = segments;
        this.pronunciationQueries = pronunciationQueries;
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public ListeningHomeView home() {
        return queries.home();
    }

    @GetMapping("/items")
    public ListeningPageView list(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int pageSize,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(required = false) Integer level,
                                  @RequestParam(required = false) String cefr,
                                  @RequestParam(required = false) Long topic,
                                  @RequestParam(required = false) Long scene,
                                  @RequestParam(required = false) Long format) {
        return queries.publicList(page, pageSize, q, level, cefr, topic, scene, format);
    }

    @GetMapping("/items/{slug}")
    public ListeningItemView detail(@PathVariable String slug) {
        return queries.publicGet(slug);
    }

    @GetMapping("/items/{slug}/segments")
    public List<ListeningSegmentView> segments(@PathVariable String slug) {
        return segments.publicList(queries.publicGet(slug).id());
    }

    @GetMapping("/items/{slug}/exercises")
    public List<ExercisePublicView> exercises(@PathVariable String slug) {
        return exerciseService.publicListPublished(queries.publicGet(slug).id());
    }

    @PostMapping("/items/{slug}/check")
    public CheckResultView check(@PathVariable String slug,
                                        @Valid @RequestBody CheckAnswerRequest request) {
        return exerciseService.check(queries.publicGet(slug).id(), request);
    }

    @GetMapping("/pronunciation")
    public List<PronunciationRuleView> rules() {
        return pronunciationQueries.list(true);
    }

    @GetMapping("/pronunciation/{slug}")
    public PronunciationRuleView rule(@PathVariable String slug) {
        return pronunciationQueries.publicGet(slug);
    }
}
