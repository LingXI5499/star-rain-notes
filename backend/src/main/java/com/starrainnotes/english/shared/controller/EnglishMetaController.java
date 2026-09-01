package com.starrainnotes.english.shared.controller;

import com.starrainnotes.english.shared.cefr.service.CefrService;
import com.starrainnotes.english.shared.dto.EnglishMetaView;
import com.starrainnotes.english.shared.exercise.service.EnglishExerciseService;
import com.starrainnotes.english.shared.taxonomy.service.EnglishTaxonomyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shared public English metadata (方案 §10.2). Aggregates the taxonomy tree,
 * the CEFR ladder and the question-type codes into one cached-friendly payload.
 */
@RestController
@RequestMapping("/api/v1/public/english")
public class EnglishMetaController {

    private final EnglishTaxonomyService taxonomyService;
    private final CefrService cefrService;
    private final EnglishExerciseService exerciseService;

    public EnglishMetaController(EnglishTaxonomyService taxonomyService,
                                 CefrService cefrService,
                                 EnglishExerciseService exerciseService) {
        this.taxonomyService = taxonomyService;
        this.cefrService = cefrService;
        this.exerciseService = exerciseService;
    }

    @GetMapping("/meta")
    public EnglishMetaView meta() {
        return new EnglishMetaView(taxonomyService.tree(), cefrService.list(), exerciseService.questionTypes());
    }
}
