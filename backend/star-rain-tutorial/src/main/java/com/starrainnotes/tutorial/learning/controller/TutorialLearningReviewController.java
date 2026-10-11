package com.starrainnotes.tutorial.learning.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.learning.dto.MasterySelfRatingDTO;
import com.starrainnotes.tutorial.learning.dto.ReviewCompleteDTO;
import com.starrainnotes.tutorial.learning.service.LearningReviewService;
import com.starrainnotes.tutorial.learning.vo.MasteryVO;
import com.starrainnotes.tutorial.learning.vo.ReviewResultVO;
import com.starrainnotes.tutorial.learning.vo.ReviewTaskVO;
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

// Legacy controller retired; the evidence controller owns the public API.
@RequiredArgsConstructor
@RequestMapping("/api/account/learning")
@PreAuthorize("isAuthenticated()")
public class TutorialLearningReviewController {
    private final LearningReviewService reviews;

    @GetMapping("/reviews/today")
    public ApiResponse<List<ReviewTaskVO>> today() {
        return ApiResponse.ok(reviews.today());
    }

    @GetMapping("/reviews/{taskId}")
    public ApiResponse<ReviewTaskVO> task(@PathVariable Long taskId) {
        return ApiResponse.ok(reviews.task(taskId));
    }

    @GetMapping("/reviews/{taskId}/back")
    public ApiResponse<Map<String, String>> back(@PathVariable Long taskId) {
        return ApiResponse.ok(Map.of("backMarkdown", reviews.back(taskId)));
    }

    @PostMapping("/reviews/{taskId}/complete")
    public ApiResponse<ReviewResultVO> complete(@PathVariable Long taskId,
            @Valid @RequestBody ReviewCompleteDTO request) {
        return ApiResponse.ok(reviews.complete(taskId, request.getRating()));
    }

    @GetMapping("/mastery/cards")
    public ApiResponse<List<MasteryVO>> mastery() {
        return ApiResponse.ok(reviews.mastery());
    }

    @PutMapping("/mastery/cards/{cardId}/self-rating")
    public ApiResponse<MasteryVO> selfRating(@PathVariable Long cardId,
            @Valid @RequestBody MasterySelfRatingDTO request) {
        return ApiResponse.ok(reviews.selfRating(cardId, request.getLevel()));
    }
}
