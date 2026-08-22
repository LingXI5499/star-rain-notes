package com.starrainnotes.english.controller;

import com.starrainnotes.english.dto.EnglishView;
import com.starrainnotes.english.dto.UpdateEnglishRequest;
import com.starrainnotes.english.service.EnglishService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin English management (04 §13). currentStage cannot be changed.
 */
@RestController
@RequestMapping("/api/v1/admin/english")
public class EnglishAdminController {

    private final EnglishService englishService;

    public EnglishAdminController(EnglishService englishService) {
        this.englishService = englishService;
    }

    @GetMapping
    public EnglishView get() {
        return englishService.get();
    }

    @PutMapping
    public EnglishView update(@Valid @RequestBody UpdateEnglishRequest request) {
        return englishService.update(request);
    }
}
