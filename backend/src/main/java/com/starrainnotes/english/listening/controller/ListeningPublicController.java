package com.starrainnotes.english.listening.controller;

import com.starrainnotes.english.listening.dto.ListeningHomeView;
import com.starrainnotes.english.listening.dto.ListeningItemView;
import com.starrainnotes.english.listening.dto.ListeningPageView;
import com.starrainnotes.english.listening.dto.ListeningSegmentView;
import com.starrainnotes.english.listening.dto.PronunciationRuleView;
import com.starrainnotes.english.listening.service.ListeningExerciseService;
import com.starrainnotes.english.listening.service.ListeningItemService;
import com.starrainnotes.english.reading.dto.ReadingCheckAnswerRequest;
import com.starrainnotes.english.reading.dto.ReadingCheckResultView;
import com.starrainnotes.english.reading.dto.ReadingExercisePublicView;
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

    private final ListeningItemService itemService;
    private final ListeningExerciseService exerciseService;

    public ListeningPublicController(ListeningItemService itemService, ListeningExerciseService exerciseService) {
        this.itemService = itemService;
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public ListeningHomeView home() {
        return itemService.home();
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
        return itemService.publicList(page, pageSize, q, level, cefr, topic, scene, format);
    }

    @GetMapping("/items/{slug}")
    public ListeningItemView detail(@PathVariable String slug) {
        return itemService.publicGet(slug);
    }

    @GetMapping("/items/{slug}/segments")
    public List<ListeningSegmentView> segments(@PathVariable String slug) {
        return itemService.publicSegments(itemService.publicGet(slug).id());
    }

    @GetMapping("/items/{slug}/exercises")
    public List<ReadingExercisePublicView> exercises(@PathVariable String slug) {
        return exerciseService.publicListPublished(itemService.publicGet(slug).id());
    }

    @PostMapping("/items/{slug}/check")
    public ReadingCheckResultView check(@PathVariable String slug,
                                        @Valid @RequestBody ReadingCheckAnswerRequest request) {
        return exerciseService.check(itemService.publicGet(slug).id(), request);
    }

    @GetMapping("/pronunciation")
    public List<PronunciationRuleView> rules() {
        return itemService.pronunciationRules(true);
    }

    @GetMapping("/pronunciation/{slug}")
    public PronunciationRuleView rule(@PathVariable String slug) {
        return itemService.publicRule(slug);
    }
}
