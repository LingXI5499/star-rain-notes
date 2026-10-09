package com.starrainnotes.tutorial.learning.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.learning.dto.*;
import com.starrainnotes.tutorial.learning.entity.EvidenceModels.*;
import com.starrainnotes.tutorial.learning.service.impl.EvidenceLearningService;
import com.starrainnotes.tutorial.learning.service.impl.EvidenceStudyPlanService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/account/learning")
public class TutorialEvidenceController {
    private final EvidenceStudyPlanService plans;
    private final EvidenceLearningService learning;

    @GetMapping("/plans") public ApiResponse<List<Plan>> plans(@RequestParam(defaultValue="false") boolean currentOnly) { return ApiResponse.ok(plans.list(currentOnly)); }
    @GetMapping("/plans/history") public ApiResponse<PageResult<Plan>> planHistory(@Valid @ModelAttribute LearningPageQueryDTO query) { return ApiResponse.ok(plans.history(query)); }
    @PostMapping("/plans/preview") public ApiResponse<Plan> preview(@Valid @RequestBody EvidencePlanDTO request,@RequestParam(required=false) Long excludePlanId) { return ApiResponse.ok(plans.preview(request,excludePlanId)); }
    @PostMapping("/plans") public ApiResponse<Plan> create(@Valid @RequestBody EvidencePlanDTO request) { return ApiResponse.ok(plans.create(request)); }
    @GetMapping("/plans/{id}") public ApiResponse<Plan> plan(@PathVariable Long id) { return ApiResponse.ok(plans.get(id)); }
    @PatchMapping("/plans/{id}") public ApiResponse<Plan> update(@PathVariable Long id,@Valid @RequestBody EvidencePlanDTO request) { return ApiResponse.ok(plans.update(id,request)); }
    @PostMapping("/plans/{id}/{action:activate|pause|resume|cancel|restart}") public ApiResponse<Plan> transition(@PathVariable Long id,@PathVariable String action) { return ApiResponse.ok(plans.transition(id,action)); }
    @GetMapping("/plans/{id}/tasks") public ApiResponse<List<Task>> tasks(@PathVariable Long id) { return ApiResponse.ok(plans.tasks(id)); }
    @GetMapping("/tasks/{id}") public ApiResponse<Task> task(@PathVariable Long id) { return ApiResponse.ok(plans.task(id)); }
    @GetMapping("/tasks/{id}/chapters") public ApiResponse<List<Chapter>> taskChapters(@PathVariable Long id) { return ApiResponse.ok(plans.task(id).getChapters()); }
    @PostMapping("/tasks/{id}/start") public ApiResponse<Task> start(@PathVariable Long id) { return ApiResponse.ok(plans.start(id)); }
    @PostMapping("/chapters/{id}/sessions") public ApiResponse<Session> initial(@PathVariable Long id,@RequestParam(required=false) Long taskId) { return ApiResponse.ok(learning.initial(id,taskId)); }
    @GetMapping("/chapters/{id}/study-state") public ApiResponse<StudyState> studyState(@PathVariable Long id,@RequestParam(required=false) Long planId) { return ApiResponse.ok(learning.studyState(id,planId)); }
    @GetMapping("/sessions/{id}") public ApiResponse<Session> session(@PathVariable Long id) { return ApiResponse.ok(learning.session(id)); }
    @PostMapping("/sessions/{id}/cards/{cardId}/reveal") public ApiResponse<Session> reveal(@PathVariable Long id,@PathVariable Long cardId) { return ApiResponse.ok(learning.reveal(id,cardId,false)); }
    @PostMapping("/sessions/{id}/cards/{cardId}/rate") public ApiResponse<Session> rate(@PathVariable Long id,@PathVariable Long cardId,@Valid @RequestBody EvidenceRatingDTO request) { return ApiResponse.ok(learning.rate(id,cardId,request,false)); }
    @GetMapping("/questions/{id}/answer") public ApiResponse<Answer> answer(@PathVariable Long id) { return ApiResponse.ok(learning.answer(id)); }
    @PostMapping("/questions/{id}/versions") public ApiResponse<Answer> save(@PathVariable Long id,@Valid @RequestBody AnswerVersionDTO request) { return ApiResponse.ok(learning.saveVersion(id,request)); }
    @GetMapping("/questions/{id}/versions") public ApiResponse<List<AnswerVersion>> versions(@PathVariable Long id) { return ApiResponse.ok(learning.versions(id)); }
    @PostMapping("/questions/{id}/unlock-reference") public ApiResponse<Map<String,Boolean>> unlock(@PathVariable Long id) { learning.reference(id);return ApiResponse.ok(Map.of("unlocked",true)); }
    @GetMapping("/questions/{id}/reference-answer") public ApiResponse<Map<String,String>> reference(@PathVariable Long id) { return ApiResponse.ok(Map.of("referenceAnswer",learning.reference(id))); }
    @GetMapping("/mastery") public ApiResponse<List<CardMastery>> mastery() { return ApiResponse.ok(learning.mastery(null,null)); }
    @GetMapping("/mastery/page") public ApiResponse<PageResult<CardMastery>> masteryPage(@Valid @ModelAttribute LearningPageQueryDTO query) { return ApiResponse.ok(learning.masteryPage(query)); }
    @GetMapping("/mastery/options") public ApiResponse<List<TutorialOption>> masteryOptions() { return ApiResponse.ok(learning.masteryOptions()); }
    @GetMapping("/mastery/tutorials/{id}") public ApiResponse<List<CardMastery>> tutorialMastery(@PathVariable Long id) { return ApiResponse.ok(learning.mastery(id,null)); }
    @GetMapping("/mastery/chapters/{id}") public ApiResponse<List<CardMastery>> chapterMastery(@PathVariable Long id) { return ApiResponse.ok(learning.mastery(null,id)); }
    @GetMapping("/reviews/summary") public ApiResponse<ReviewSummary> summary() { return ApiResponse.ok(learning.reviewSummary()); }
    @PostMapping("/reviews/sessions") public ApiResponse<Session> review(@Valid @RequestBody ReviewSessionDTO request) { return ApiResponse.ok(learning.review(request)); }
    @GetMapping("/reviews/sessions/current") public ApiResponse<Session> current() { return ApiResponse.ok(learning.currentReview()); }
    @GetMapping("/reviews/sessions/{id}") public ApiResponse<Session> reviewSession(@PathVariable Long id) { return ApiResponse.ok(learning.reviewSession(id)); }
    @PostMapping("/reviews/sessions/{id}/cards/{cardId}/reveal") public ApiResponse<Session> reviewReveal(@PathVariable Long id,@PathVariable Long cardId) { return ApiResponse.ok(learning.reveal(id,cardId,true)); }
    @PostMapping("/reviews/sessions/{id}/cards/{cardId}/rate") public ApiResponse<Session> reviewRate(@PathVariable Long id,@PathVariable Long cardId,@Valid @RequestBody EvidenceRatingDTO request) { return ApiResponse.ok(learning.rate(id,cardId,request,true)); }
    @PostMapping("/reviews/sessions/{id}/abandon") public ApiResponse<Session> abandon(@PathVariable Long id) { return ApiResponse.ok(learning.abandon(id)); }
    @GetMapping("/history") public ApiResponse<PageResult<Session>> history(@Valid @ModelAttribute LearningPageQueryDTO query) { return ApiResponse.ok(learning.history(query)); }
    @GetMapping("/history/options") public ApiResponse<List<TutorialOption>> historyOptions() { return ApiResponse.ok(learning.historyOptions()); }
    @GetMapping("/statistics") public ApiResponse<Statistics> statistics() { return ApiResponse.ok(learning.statistics()); }
}
