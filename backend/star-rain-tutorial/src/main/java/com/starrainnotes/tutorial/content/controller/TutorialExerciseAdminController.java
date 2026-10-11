package com.starrainnotes.tutorial.content.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.content.service.TutorialOperationService;
import com.starrainnotes.tutorial.content.constant.TutorialPermissions;
import com.starrainnotes.tutorial.content.dto.ChapterQuestionDTO;
import com.starrainnotes.tutorial.content.dto.IdOrderDTO;
import com.starrainnotes.tutorial.content.dto.KnowledgeCardDTO;
import com.starrainnotes.tutorial.content.service.TutorialExerciseService;
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
    private final TutorialOperationService operations;
    private final TutorialExerciseService service;

    @GetMapping("/tutorial-chapters/{chapterId}/cards")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<List<Map<String, Object>>> cards(@PathVariable Long chapterId) {
        return ApiResponse.ok(service.cards(chapterId));
    }

    @PostMapping("/tutorial-chapters/{chapterId}/cards")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> createCard(@PathVariable Long chapterId,
                                                        @Valid @RequestBody KnowledgeCardDTO request) {
        return ApiResponse.ok(operations.request("createCard", new Object[]{chapterId, request}));
    }

    @PatchMapping("/tutorial-cards/{cardId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> updateCard(@PathVariable Long cardId,
                                                        @Valid @RequestBody KnowledgeCardDTO request) {
        return ApiResponse.ok(operations.request("updateCard", new Object[]{cardId, request}));
    }

    @DeleteMapping("/tutorial-cards/{cardId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> deleteCard(@PathVariable Long cardId) {
        return ApiResponse.ok(operations.request("deleteCard", new Object[]{cardId}));
    }

    @PutMapping("/tutorial-chapters/{chapterId}/cards/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> reorderCards(@PathVariable Long chapterId, @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(operations.request("reorderCards", new Object[]{chapterId, request.getIds()}));
    }

    @GetMapping("/tutorial-chapters/{chapterId}/questions")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<List<Map<String, Object>>> questions(@PathVariable Long chapterId) {
        return ApiResponse.ok(service.questions(chapterId));
    }

    @PostMapping("/tutorial-chapters/{chapterId}/questions")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> createQuestion(@PathVariable Long chapterId,
                                                            @Valid @RequestBody ChapterQuestionDTO request) {
        return ApiResponse.ok(operations.request("createQuestion", new Object[]{chapterId, request}));
    }

    @PatchMapping("/tutorial-questions/{questionId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> updateQuestion(@PathVariable Long questionId,
                                                            @Valid @RequestBody ChapterQuestionDTO request) {
        return ApiResponse.ok(operations.request("updateQuestion", new Object[]{questionId, request}));
    }

    @DeleteMapping("/tutorial-questions/{questionId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> deleteQuestion(@PathVariable Long questionId) {
        return ApiResponse.ok(operations.request("deleteQuestion", new Object[]{questionId}));
    }

    @PutMapping("/tutorial-chapters/{chapterId}/questions/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> reorderQuestions(@PathVariable Long chapterId, @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(operations.request("reorderQuestions", new Object[]{chapterId, request.getIds()}));
    }
}
