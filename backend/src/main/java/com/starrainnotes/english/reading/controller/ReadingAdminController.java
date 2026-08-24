package com.starrainnotes.english.reading.controller;

import com.starrainnotes.english.reading.dto.ReadingArticleRequest;
import com.starrainnotes.english.reading.dto.ReadingArticleView;
import com.starrainnotes.english.reading.dto.ReadingExerciseMoveRequest;
import com.starrainnotes.english.reading.dto.ReadingExerciseRequest;
import com.starrainnotes.english.reading.dto.ReadingExerciseView;
import com.starrainnotes.english.reading.dto.ReadingPageView;
import com.starrainnotes.english.reading.service.ReadingArticleService;
import com.starrainnotes.english.reading.service.ReadingExerciseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin reading management (方案 §10.3). Writes go through the frozen session +
 * CSRF flow; DRAFT-only fields are never leaked to public clients.
 */
@RestController
@RequestMapping("/api/v1/admin/english/reading")
public class ReadingAdminController {

    private final ReadingArticleService articleService;
    private final ReadingExerciseService exerciseService;

    public ReadingAdminController(ReadingArticleService articleService,
                                  ReadingExerciseService exerciseService) {
        this.articleService = articleService;
        this.exerciseService = exerciseService;
    }

    @GetMapping("/articles")
    public ReadingPageView list(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int pageSize,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) Integer level,
                                @RequestParam(required = false) String cefr,
                                @RequestParam(required = false) Long topic,
                                @RequestParam(required = false) Long genre) {
        return articleService.list(page, pageSize, q, status, level, cefr, topic, genre);
    }

    @PostMapping("/articles")
    @ResponseStatus(HttpStatus.CREATED)
    public ReadingArticleView create(@Valid @RequestBody ReadingArticleRequest request) {
        return articleService.create(request);
    }

    @GetMapping("/articles/{id}")
    public ReadingArticleView get(@PathVariable Long id) {
        return articleService.get(id);
    }

    @PutMapping("/articles/{id}")
    public ReadingArticleView update(@PathVariable Long id, @Valid @RequestBody ReadingArticleRequest request) {
        return articleService.update(id, request);
    }

    @DeleteMapping("/articles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        articleService.delete(id);
    }

    @PostMapping("/articles/{id}/publish")
    public ReadingArticleView publish(@PathVariable Long id) {
        return articleService.publish(id);
    }

    @PostMapping("/articles/{id}/withdraw")
    public ReadingArticleView withdraw(@PathVariable Long id) {
        return articleService.withdraw(id);
    }

    @GetMapping("/articles/{id}/exercises")
    public List<ReadingExerciseView> exercises(@PathVariable Long id) {
        return exerciseService.listByArticle(id);
    }

    @PostMapping("/articles/{id}/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public ReadingExerciseView createExercise(@PathVariable Long id,
                                              @Valid @RequestBody ReadingExerciseRequest request) {
        return exerciseService.create(id, request);
    }

    @PutMapping("/articles/{id}/exercises/{exerciseId}")
    public ReadingExerciseView updateExercise(@PathVariable Long id, @PathVariable Long exerciseId,
                                              @Valid @RequestBody ReadingExerciseRequest request) {
        return exerciseService.update(id, exerciseId, request);
    }

    @PostMapping("/articles/{id}/exercises/{exerciseId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveExercise(@PathVariable Long id, @PathVariable Long exerciseId,
                             @RequestBody ReadingExerciseMoveRequest request) {
        exerciseService.move(id, exerciseId,
                request.targetIndex() == null ? 0 : request.targetIndex());
    }

    @DeleteMapping("/articles/{id}/exercises/{exerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExercise(@PathVariable Long id, @PathVariable Long exerciseId) {
        exerciseService.delete(id, exerciseId);
    }
}
