package com.starrainnotes.english.grammar.controller;

import com.starrainnotes.account.review.dto.ContentReviewView;
import com.starrainnotes.account.review.service.ContentReviewService;
import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.english.grammar.dto.GrammarCourseView;
import com.starrainnotes.english.grammar.dto.GrammarCurriculumView;
import com.starrainnotes.english.grammar.dto.GrammarLessonDetailView;
import com.starrainnotes.english.grammar.dto.GrammarLessonRequest;
import com.starrainnotes.english.grammar.dto.GrammarMoveRequest;
import com.starrainnotes.english.grammar.dto.GrammarReassignRequest;
import com.starrainnotes.english.grammar.dto.GrammarSectionRequest;
import com.starrainnotes.english.grammar.dto.GrammarSectionView;
import com.starrainnotes.english.grammar.dto.UpdateGrammarCourseRequest;
import com.starrainnotes.english.grammar.application.GrammarCommandService;
import com.starrainnotes.english.grammar.application.GrammarQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    private final GrammarQueryService queries;
    private final GrammarCommandService commands;
    private final ContentReviewService reviewService;

    public EnglishGrammarAdminController(GrammarQueryService queries, GrammarCommandService commands, ContentReviewService reviewService) {
        this.queries = queries;
        this.commands = commands;
        this.reviewService = reviewService;
    }

    @GetMapping
    public GrammarCourseView course() { return queries.course(); }

    @PutMapping
    public GrammarCourseView updateCourse(@Valid @RequestBody UpdateGrammarCourseRequest request) {
        return commands.updateCourse(request);
    }

    @PostMapping("/publish")
    public GrammarCourseView publishCourse() { return commands.publishCourse(); }

    @PostMapping("/withdraw")
    public GrammarCourseView withdrawCourse() { return commands.withdrawCourse(); }

    @GetMapping("/curriculum")
    public GrammarCurriculumView curriculum() { return queries.curriculum(); }

    @PostMapping("/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public GrammarSectionView createSection(@Valid @RequestBody GrammarSectionRequest request) {
        return commands.createSection(request);
    }

    @PutMapping("/sections/{sectionId}")
    public GrammarSectionView updateSection(@PathVariable long sectionId,
                                            @Valid @RequestBody GrammarSectionRequest request) {
        return commands.updateSection(sectionId, request);
    }

    @DeleteMapping("/sections/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(@PathVariable long sectionId) { commands.deleteSection(sectionId); }

    @PostMapping("/sections/{sectionId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveSection(@PathVariable long sectionId, @Valid @RequestBody GrammarMoveRequest request) {
        commands.moveSection(sectionId, request);
    }

    @PostMapping("/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public GrammarLessonDetailView createLesson(@Valid @RequestBody GrammarLessonRequest request) {
        return commands.createLesson(request);
    }

    @GetMapping("/lessons/{lessonId}")
    public GrammarLessonDetailView lesson(@PathVariable long lessonId) { return queries.lesson(lessonId); }

    @PutMapping("/lessons/{lessonId}")
    public ResponseEntity<?> updateLesson(@PathVariable long lessonId,
                                          @Valid @RequestBody GrammarLessonRequest request,
                                          Authentication authentication) {
        if (!isSuperAdmin(authentication) && reviewService.isPublished("ENGLISH_GRAMMAR_LESSON", lessonId)) {
            ContentReviewView review = reviewService.submitEnglishUpdate(actorId(authentication),
                    "ENGLISH_GRAMMAR_LESSON", lessonId, request.title(), request);
            return ResponseEntity.accepted().body(review);
        }
        return ResponseEntity.ok(commands.updateLesson(lessonId, request));
    }

    @DeleteMapping("/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLesson(@PathVariable long lessonId) { commands.deleteLesson(lessonId); }

    @PostMapping("/lessons/{lessonId}/publish")
    public GrammarLessonDetailView publishLesson(@PathVariable long lessonId) {
        return commands.publishLesson(lessonId);
    }

    @PostMapping("/lessons/{lessonId}/withdraw")
    public GrammarLessonDetailView withdrawLesson(@PathVariable long lessonId) {
        return commands.withdrawLesson(lessonId);
    }

    @PostMapping("/lessons/{lessonId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveLesson(@PathVariable long lessonId, @Valid @RequestBody GrammarMoveRequest request) {
        commands.moveLesson(lessonId, request);
    }

    @PostMapping("/lessons/{lessonId}/reassign")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reassignLesson(@PathVariable long lessonId,
                               @Valid @RequestBody GrammarReassignRequest request) {
        commands.reassignLesson(lessonId, request);
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    private Long actorId(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();
        return principal instanceof AccountPrincipal account ? account.getId() : null;
    }
}
