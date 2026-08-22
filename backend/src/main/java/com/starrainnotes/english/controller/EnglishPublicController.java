package com.starrainnotes.english.controller;

import com.starrainnotes.english.dto.EnglishView;
import com.starrainnotes.english.service.EnglishService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public English entry (04 §13).
 */
@RestController
@RequestMapping("/api/v1/public/english")
public class EnglishPublicController {

    private final EnglishService englishService;

    public EnglishPublicController(EnglishService englishService) {
        this.englishService = englishService;
    }

    @GetMapping
    public EnglishView get() {
        return englishService.get();
    }
}
