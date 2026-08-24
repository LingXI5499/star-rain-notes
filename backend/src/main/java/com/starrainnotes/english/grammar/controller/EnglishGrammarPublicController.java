package com.starrainnotes.english.grammar.controller;

import com.starrainnotes.english.grammar.dto.GrammarCurriculumView;
import com.starrainnotes.english.grammar.dto.GrammarLessonDetailView;
import com.starrainnotes.english.grammar.service.EnglishGrammarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/english/grammar")
public class EnglishGrammarPublicController {

    private final EnglishGrammarService service;

    public EnglishGrammarPublicController(EnglishGrammarService service) {
        this.service = service;
    }

    @GetMapping
    public GrammarCurriculumView curriculum() { return service.publicCurriculum(); }

    @GetMapping("/lessons/{slug}")
    public GrammarLessonDetailView lesson(@PathVariable String slug) { return service.publicLesson(slug); }
}
