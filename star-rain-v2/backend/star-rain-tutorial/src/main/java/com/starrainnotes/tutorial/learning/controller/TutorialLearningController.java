package com.starrainnotes.tutorial.learning.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.learning.dto.LearningAnswerDTO;
import com.starrainnotes.tutorial.learning.dto.LearningProgressDTO;
import com.starrainnotes.tutorial.learning.service.LearningAnswerService;
import com.starrainnotes.tutorial.learning.service.LearningProgressService;
import com.starrainnotes.tutorial.learning.vo.LearningAnswerVO;
import com.starrainnotes.tutorial.learning.vo.LearningProgressVO;
import com.starrainnotes.tutorial.learning.vo.LearningTutorialProgressVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
    private final LearningAnswerService answerService;

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

    @PutMapping("/questions/{questionId}/answer")
    public ApiResponse<LearningAnswerVO> answer(@PathVariable Long questionId,
            @Valid @RequestBody LearningAnswerDTO request) {
        return ApiResponse.ok(answerService.answer(questionId, request));
    }

    @GetMapping("/questions/{questionId}/answer")
    public ApiResponse<LearningAnswerVO> ownAnswer(@PathVariable Long questionId) {
        return ApiResponse.ok(answerService.ownAnswer(questionId));
    }

    @GetMapping("/questions/{questionId}/reference-answer")
    public ApiResponse<Map<String, String>> referenceAnswer(@PathVariable Long questionId) {
        return ApiResponse.ok(Map.of("referenceAnswer", answerService.referenceAnswer(questionId)));
    }
}
