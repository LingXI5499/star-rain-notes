package com.starrainnotes.tutorial.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.constant.TutorialPermissions;
import com.starrainnotes.tutorial.dto.ChapterQuestionDTO;
import com.starrainnotes.tutorial.dto.IdOrderDTO;
import com.starrainnotes.tutorial.dto.KnowledgeCardDTO;
import com.starrainnotes.tutorial.service.TutorialExerciseService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class TutorialExerciseAdminController {
    private final TutorialExerciseService service;

    @GetMapping("/tutorial-chapters/{chapterId}/cards")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<List<Map<String, Object>>> cards(@PathVariable Long chapterId) {
        return ApiResponse.ok(service.cards(chapterId));
    }

    @PostMapping("/tutorial-chapters/{chapterId}/cards")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Map<String, Object>> createCard(@PathVariable Long chapterId,
                                                        @Valid @RequestBody KnowledgeCardDTO request) {
        return ApiResponse.ok(service.createCard(chapterId, request));
    }

    @PatchMapping("/tutorial-cards/{cardId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Map<String, Object>> updateCard(@PathVariable Long cardId,
                                                        @Valid @RequestBody KnowledgeCardDTO request) {
        return ApiResponse.ok(service.updateCard(cardId, request));
    }

    @DeleteMapping("/tutorial-cards/{cardId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Void> deleteCard(@PathVariable Long cardId) {
        service.deleteCard(cardId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/tutorial-chapters/{chapterId}/cards/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Void> reorderCards(@PathVariable Long chapterId, @Valid @RequestBody IdOrderDTO request) {
        service.reorderCards(chapterId, request.getIds());
        return ApiResponse.ok(null);
    }

    @GetMapping("/tutorial-chapters/{chapterId}/questions")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<List<Map<String, Object>>> questions(@PathVariable Long chapterId) {
        return ApiResponse.ok(service.questions(chapterId));
    }

    @PostMapping("/tutorial-chapters/{chapterId}/questions")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Map<String, Object>> createQuestion(@PathVariable Long chapterId,
                                                            @Valid @RequestBody ChapterQuestionDTO request) {
        return ApiResponse.ok(service.createQuestion(chapterId, request));
    }

    @PatchMapping("/tutorial-questions/{questionId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Map<String, Object>> updateQuestion(@PathVariable Long questionId,
                                                            @Valid @RequestBody ChapterQuestionDTO request) {
        return ApiResponse.ok(service.updateQuestion(questionId, request));
    }

    @DeleteMapping("/tutorial-questions/{questionId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Void> deleteQuestion(@PathVariable Long questionId) {
        service.deleteQuestion(questionId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/tutorial-chapters/{chapterId}/questions/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Void> reorderQuestions(@PathVariable Long chapterId, @Valid @RequestBody IdOrderDTO request) {
        service.reorderQuestions(chapterId, request.getIds());
        return ApiResponse.ok(null);
    }
}
