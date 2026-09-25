package com.starrainnotes.english.writing.controller;

import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.english.shared.exercise.dto.ExerciseRequest;
import com.starrainnotes.english.shared.exercise.dto.ExerciseView;
import com.starrainnotes.english.writing.dto.WritingMoveRequest;
import com.starrainnotes.english.writing.dto.WritingPageView;
import com.starrainnotes.english.writing.dto.WritingPromptRequest;
import com.starrainnotes.english.writing.dto.WritingPromptSummaryView;
import com.starrainnotes.english.writing.dto.WritingPromptView;
import com.starrainnotes.english.writing.dto.WritingResourceRequest;
import com.starrainnotes.english.writing.dto.WritingResourceSummaryView;
import com.starrainnotes.english.writing.dto.WritingResourceView;
import com.starrainnotes.english.writing.application.WritingExerciseApplicationService;
import com.starrainnotes.english.writing.application.WritingPromptCommandService;
import com.starrainnotes.english.writing.application.WritingPromptQueryService;
import com.starrainnotes.english.writing.application.WritingResourceCommandService;
import com.starrainnotes.english.writing.application.WritingResourceQueryService;
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

@RestController
@RequestMapping("/api/v1/admin/english/writing")
public class WritingAdminController {

    private final WritingResourceQueryService resources;
    private final WritingResourceCommandService resourceCommands;
    private final WritingPromptQueryService prompts;
    private final WritingPromptCommandService promptCommands;
    private final WritingExerciseApplicationService exercises;

    public WritingAdminController(WritingResourceQueryService resources, WritingResourceCommandService resourceCommands,
                                  WritingPromptQueryService prompts, WritingPromptCommandService promptCommands,
                                  WritingExerciseApplicationService exercises) {
        this.resources = resources;
        this.resourceCommands = resourceCommands;
        this.prompts = prompts;
        this.promptCommands = promptCommands;
        this.exercises = exercises;
    }

    @GetMapping("/resources")
    public WritingPageView<WritingResourceSummaryView> resources(@RequestParam(defaultValue = "1") int page,
                                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                                 @RequestParam(required = false) String q,
                                                                 @RequestParam(required = false) String kind,
                                                                 @RequestParam(required = false) String expressionLevel,
                                                                 @RequestParam(required = false) String cefr,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(required = false) Long topic,
                                                                 @RequestParam(required = false) Long genre) {
        return resources.list(page, pageSize, q, kind, expressionLevel, cefr, status, topic, genre);
    }

    @PostMapping("/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public WritingResourceView createResource(@Valid @RequestBody WritingResourceRequest request) {
        return resourceCommands.create(request);
    }

    @GetMapping("/resources/{id}")
    public WritingResourceView resource(@PathVariable Long id) {
        return resources.get(id);
    }

    @PutMapping("/resources/{id}")
    public ResponseEntity<?> updateResource(@PathVariable Long id,
                                            @Valid @RequestBody WritingResourceRequest request,
                                            Authentication authentication) {
        var result = resourceCommands.updateForEditor(actorId(authentication), isSuperAdmin(authentication), id, request);
        return ResponseEntity.status(result.submitted() ? HttpStatus.ACCEPTED : HttpStatus.OK).body(result.body());
    }

    @DeleteMapping("/resources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@PathVariable Long id) {
        resourceCommands.delete(id);
    }

    @PostMapping("/resources/{id}/publish")
    public WritingResourceView publishResource(@PathVariable Long id) {
        return resourceCommands.publish(id);
    }

    @PostMapping("/resources/{id}/withdraw")
    public WritingResourceView withdrawResource(@PathVariable Long id) {
        return resourceCommands.withdraw(id);
    }

    @PostMapping("/resources/{id}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveResource(@PathVariable Long id, @Valid @RequestBody WritingMoveRequest request) {
        resourceCommands.move(id, request.targetIndex());
    }

    @GetMapping("/prompts")
    public WritingPageView<WritingPromptSummaryView> prompts(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int pageSize,
                                                             @RequestParam(required = false) String q,
                                                             @RequestParam(required = false) String cefr,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) Long topic,
                                                             @RequestParam(required = false) Long genre) {
        return prompts.list(page, pageSize, q, cefr, status, topic, genre);
    }

    @PostMapping("/prompts")
    @ResponseStatus(HttpStatus.CREATED)
    public WritingPromptView createPrompt(@Valid @RequestBody WritingPromptRequest request) {
        return promptCommands.create(request);
    }

    @GetMapping("/prompts/{id}")
    public WritingPromptView prompt(@PathVariable Long id) {
        return prompts.get(id);
    }

    @PutMapping("/prompts/{id}")
    public ResponseEntity<?> updatePrompt(@PathVariable Long id,
                                          @Valid @RequestBody WritingPromptRequest request,
                                          Authentication authentication) {
        var result = promptCommands.updateForEditor(actorId(authentication), isSuperAdmin(authentication), id, request);
        return ResponseEntity.status(result.submitted() ? HttpStatus.ACCEPTED : HttpStatus.OK).body(result.body());
    }

    @DeleteMapping("/prompts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePrompt(@PathVariable Long id) {
        promptCommands.delete(id);
    }

    @PostMapping("/prompts/{id}/publish")
    public WritingPromptView publishPrompt(@PathVariable Long id) {
        return promptCommands.publish(id);
    }

    @PostMapping("/prompts/{id}/withdraw")
    public WritingPromptView withdrawPrompt(@PathVariable Long id) {
        return promptCommands.withdraw(id);
    }

    @PostMapping("/prompts/{id}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void movePrompt(@PathVariable Long id, @Valid @RequestBody WritingMoveRequest request) {
        promptCommands.move(id, request.targetIndex());
    }

    @GetMapping("/prompts/{id}/exercises")
    public List<ExerciseView> exercises(@PathVariable Long id) {
        return exercises.list(id);
    }

    @PostMapping("/prompts/{id}/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseView createExercise(@PathVariable Long id,
                                              @Valid @RequestBody ExerciseRequest request) {
        return exercises.create(id, request);
    }

    @PutMapping("/prompts/{id}/exercises/{exerciseId}")
    public ExerciseView updateExercise(@PathVariable Long id,
                                              @PathVariable Long exerciseId,
                                              @Valid @RequestBody ExerciseRequest request) {
        return exercises.update(id, exerciseId, request);
    }

    @PostMapping("/prompts/{id}/exercises/{exerciseId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveExercise(@PathVariable Long id, @PathVariable Long exerciseId,
                             @Valid @RequestBody WritingMoveRequest request) {
        exercises.move(id, exerciseId, request.targetIndex());
    }

    @DeleteMapping("/prompts/{id}/exercises/{exerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExercise(@PathVariable Long id, @PathVariable Long exerciseId) {
        exercises.delete(id, exerciseId);
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
