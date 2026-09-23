package com.starrainnotes.english.reading.controller;

import com.starrainnotes.account.review.dto.ContentReviewView;
import com.starrainnotes.account.review.service.ContentReviewService;
import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.english.reading.dto.ReadingArticleRequest;
import com.starrainnotes.english.reading.dto.ReadingArticleView;
import com.starrainnotes.english.shared.exercise.dto.ExerciseMoveRequest;
import com.starrainnotes.english.shared.exercise.dto.ExerciseRequest;
import com.starrainnotes.english.shared.exercise.dto.ExerciseView;
import com.starrainnotes.english.reading.dto.ReadingPageView;
import com.starrainnotes.english.reading.application.ReadingCommandService;
import com.starrainnotes.english.reading.application.ReadingQueryService;
import com.starrainnotes.english.reading.application.ReadingExerciseApplicationService;
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

    private final ReadingCommandService articleService;
    private final ReadingQueryService queries;
    private final ReadingExerciseApplicationService exerciseService;
    private final ContentReviewService reviewService;

    public ReadingAdminController(ReadingCommandService articleService, ReadingQueryService queries,
                                  ReadingExerciseApplicationService exerciseService,
                                  ContentReviewService reviewService) {
        this.articleService = articleService;
        this.queries = queries;
        this.exerciseService = exerciseService;
        this.reviewService = reviewService;
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
        return queries.list(page, pageSize, q, status, level, cefr, topic, genre);
    }

    @PostMapping("/articles")
    @ResponseStatus(HttpStatus.CREATED)
    public ReadingArticleView create(@Valid @RequestBody ReadingArticleRequest request) {
        return articleService.create(request);
    }

    @GetMapping("/articles/{id}")
    public ReadingArticleView get(@PathVariable Long id) {
        return queries.get(id);
    }

    @PutMapping("/articles/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ReadingArticleRequest request,
                                    Authentication authentication) {
        if (!isSuperAdmin(authentication) && reviewService.isPublished("ENGLISH_READING_ARTICLE", id)) {
            ContentReviewView review = reviewService.submitEnglishUpdate(actorId(authentication),
                    "ENGLISH_READING_ARTICLE", id, request.title(), request);
            return ResponseEntity.accepted().body(review);
        }
        return ResponseEntity.ok(articleService.update(id, request));
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
    public List<ExerciseView> exercises(@PathVariable Long id) {
        return exerciseService.listByArticle(id);
    }

    @PostMapping("/articles/{id}/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseView createExercise(@PathVariable Long id,
                                              @Valid @RequestBody ExerciseRequest request) {
        return exerciseService.create(id, request);
    }

    @PutMapping("/articles/{id}/exercises/{exerciseId}")
    public ExerciseView updateExercise(@PathVariable Long id, @PathVariable Long exerciseId,
                                              @Valid @RequestBody ExerciseRequest request) {
        return exerciseService.update(id, exerciseId, request);
    }

    @PostMapping("/articles/{id}/exercises/{exerciseId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveExercise(@PathVariable Long id, @PathVariable Long exerciseId,
                             @RequestBody ExerciseMoveRequest request) {
        exerciseService.move(id, exerciseId,
                request.targetIndex() == null ? 0 : request.targetIndex());
    }

    @DeleteMapping("/articles/{id}/exercises/{exerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExercise(@PathVariable Long id, @PathVariable Long exerciseId) {
        exerciseService.delete(id, exerciseId);
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
