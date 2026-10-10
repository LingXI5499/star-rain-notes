package com.starrainnotes.tutorial.content.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.content.service.TutorialOperationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TutorialOperationAdminController {
    private final TutorialOperationService operations;

    @GetMapping("/api/admin/tutorial-operations/pending")
    @PreAuthorize("hasAuthority('tutorial:read-admin')")
    public ApiResponse<List<Map<String, Object>>> pending() {
        return ApiResponse.ok(operations.pendingOperations());
    }
}
