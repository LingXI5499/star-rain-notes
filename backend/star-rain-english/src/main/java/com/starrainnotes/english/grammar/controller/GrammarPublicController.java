package com.starrainnotes.english.grammar.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.grammar.dto.GrammarDto;
import com.starrainnotes.english.grammar.service.GrammarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/english/grammar")
public class GrammarPublicController {
    private final GrammarService service;

    @GetMapping
    public ApiResponse<GrammarDto.Curriculum> curriculum() {
        return ApiResponse.ok(service.curriculum(false));
    }

    @GetMapping("/lessons/{slug}")
    public ApiResponse<GrammarDto.Lesson> lesson(@PathVariable String slug) {
        return ApiResponse.ok(service.lesson(slug, false));
    }
}
