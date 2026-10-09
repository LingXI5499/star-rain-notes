package com.starrainnotes.tutorial.learning.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.learning.dto.StudyPlanDTO;
import com.starrainnotes.tutorial.learning.service.StudyPlanService;
import com.starrainnotes.tutorial.learning.vo.StudyPlanVO;
import com.starrainnotes.tutorial.learning.vo.StudyTaskVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Legacy controller retired; the evidence controller owns the public API.
@RequiredArgsConstructor
@RequestMapping("/api/account/learning")
@PreAuthorize("isAuthenticated()")
public class TutorialStudyPlanController {
    private final StudyPlanService plans;

    @PostMapping("/plans")
    public ApiResponse<StudyPlanVO> create(@Valid @RequestBody StudyPlanDTO request) {
        return ApiResponse.ok(plans.create(request));
    }

    @GetMapping("/plans")
    public ApiResponse<List<StudyPlanVO>> list() {
        return ApiResponse.ok(plans.plans());
    }

    @GetMapping("/plans/{planId}")
    public ApiResponse<StudyPlanVO> detail(@PathVariable Long planId) {
        return ApiResponse.ok(plans.plan(planId));
    }

    @PatchMapping("/plans/{planId}")
    public ApiResponse<StudyPlanVO> update(@PathVariable Long planId, @Valid @RequestBody StudyPlanDTO request) {
        return ApiResponse.ok(plans.update(planId, request));
    }

    @PostMapping("/plans/{planId}/{action:activate|pause|resume|finish|cancel}")
    public ApiResponse<StudyPlanVO> transition(@PathVariable Long planId, @PathVariable String action) {
        return ApiResponse.ok(plans.transition(planId, action));
    }

    @GetMapping("/plans/{planId}/tasks")
    public ApiResponse<List<StudyTaskVO>> planTasks(@PathVariable Long planId) {
        return ApiResponse.ok(plans.planTasks(planId));
    }

    @GetMapping("/tasks/today")
    public ApiResponse<List<StudyTaskVO>> today() {
        return ApiResponse.ok(plans.today());
    }

    @PostMapping("/tasks/{taskId}/start")
    public ApiResponse<StudyTaskVO> start(@PathVariable Long taskId) {
        return ApiResponse.ok(plans.start(taskId));
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ApiResponse<StudyTaskVO> complete(@PathVariable Long taskId) {
        return ApiResponse.ok(plans.complete(taskId));
    }

    @PostMapping("/tasks/{taskId}/skip")
    public ApiResponse<StudyTaskVO> skip(@PathVariable Long taskId) {
        return ApiResponse.ok(plans.skip(taskId));
    }
}
