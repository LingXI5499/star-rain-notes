package com.starrainnotes.tutorial.learning.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.learning.dto.LearningProgressDTO;
import com.starrainnotes.tutorial.learning.service.LearningProgressService;
import com.starrainnotes.tutorial.learning.vo.LearningProgressVO;
import com.starrainnotes.tutorial.learning.vo.LearningTutorialProgressVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account/learning")
@PreAuthorize("isAuthenticated()")
public class TutorialLearningController {
    private final LearningProgressService progressService;

    @PutMapping("/progress/chapters/{chapterId}")
    public ApiResponse<LearningProgressVO> saveProgress(@PathVariable Long chapterId,
            @Valid @RequestBody LearningProgressDTO request) {
        return ApiResponse.ok(progressService.save(chapterId, request));
    }

    @PostMapping("/progress/chapters/{chapterId}/complete")
    public ApiResponse<LearningProgressVO> completeChapter(@PathVariable Long chapterId) {
        return ApiResponse.ok(progressService.complete(chapterId));
    }

    @GetMapping("/progress/chapters/{chapterId}")
    public ApiResponse<LearningProgressVO> chapterProgress(@PathVariable Long chapterId) {
        return ApiResponse.ok(progressService.chapter(chapterId));
    }

    @GetMapping("/progress/tutorials/{tutorialId}")
    public ApiResponse<LearningTutorialProgressVO> tutorialProgress(@PathVariable Long tutorialId) {
        return ApiResponse.ok(progressService.tutorial(tutorialId));
    }

    @GetMapping("/recent")
    public ApiResponse<List<LearningProgressVO>> recent() {
        return ApiResponse.ok(progressService.recent());
    }

}
