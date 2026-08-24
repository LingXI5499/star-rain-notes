package com.starrainnotes.english.grammar.controller;

import com.starrainnotes.english.grammar.dto.GrammarCourseView;
import com.starrainnotes.english.grammar.dto.GrammarCurriculumView;
import com.starrainnotes.english.grammar.dto.GrammarLessonDetailView;
import com.starrainnotes.english.grammar.dto.GrammarLessonRequest;
import com.starrainnotes.english.grammar.dto.GrammarMoveRequest;
import com.starrainnotes.english.grammar.dto.GrammarReassignRequest;
import com.starrainnotes.english.grammar.dto.GrammarSectionRequest;
import com.starrainnotes.english.grammar.dto.GrammarSectionView;
import com.starrainnotes.english.grammar.dto.UpdateGrammarCourseRequest;
import com.starrainnotes.english.grammar.service.EnglishGrammarService;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/v1/admin/english/grammar")
public class EnglishGrammarAdminController {

    private final EnglishGrammarService service;

    public EnglishGrammarAdminController(EnglishGrammarService service) {
        this.service = service;
    }

    @GetMapping
    public GrammarCourseView course() { return service.course(); }

    @PutMapping
    public GrammarCourseView updateCourse(@Valid @RequestBody UpdateGrammarCourseRequest request) {
        return service.updateCourse(request);
    }

    @PostMapping("/publish")
    public GrammarCourseView publishCourse() { return service.publishCourse(); }

    @PostMapping("/withdraw")
    public GrammarCourseView withdrawCourse() { return service.withdrawCourse(); }

    @GetMapping("/curriculum")
    public GrammarCurriculumView curriculum() { return service.curriculum(); }

    @PostMapping("/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public GrammarSectionView createSection(@Valid @RequestBody GrammarSectionRequest request) {
        return service.createSection(request);
    }

    @PutMapping("/sections/{sectionId}")
    public GrammarSectionView updateSection(@PathVariable long sectionId,
                                            @Valid @RequestBody GrammarSectionRequest request) {
        return service.updateSection(sectionId, request);
    }

    @DeleteMapping("/sections/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(@PathVariable long sectionId) { service.deleteSection(sectionId); }

    @PostMapping("/sections/{sectionId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveSection(@PathVariable long sectionId, @Valid @RequestBody GrammarMoveRequest request) {
        service.moveSection(sectionId, request);
    }

    @PostMapping("/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public GrammarLessonDetailView createLesson(@Valid @RequestBody GrammarLessonRequest request) {
        return service.createLesson(request);
    }

    @GetMapping("/lessons/{lessonId}")
    public GrammarLessonDetailView lesson(@PathVariable long lessonId) { return service.lesson(lessonId); }

    @PutMapping("/lessons/{lessonId}")
    public GrammarLessonDetailView updateLesson(@PathVariable long lessonId,
                                                @Valid @RequestBody GrammarLessonRequest request) {
        return service.updateLesson(lessonId, request);
    }

    @DeleteMapping("/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLesson(@PathVariable long lessonId) { service.deleteLesson(lessonId); }

    @PostMapping("/lessons/{lessonId}/publish")
    public GrammarLessonDetailView publishLesson(@PathVariable long lessonId) {
        return service.publishLesson(lessonId);
    }

    @PostMapping("/lessons/{lessonId}/withdraw")
    public GrammarLessonDetailView withdrawLesson(@PathVariable long lessonId) {
        return service.withdrawLesson(lessonId);
    }

    @PostMapping("/lessons/{lessonId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveLesson(@PathVariable long lessonId, @Valid @RequestBody GrammarMoveRequest request) {
        service.moveLesson(lessonId, request);
    }

    @PostMapping("/lessons/{lessonId}/reassign")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reassignLesson(@PathVariable long lessonId,
                               @Valid @RequestBody GrammarReassignRequest request) {
        service.reassignLesson(lessonId, request);
    }
}
