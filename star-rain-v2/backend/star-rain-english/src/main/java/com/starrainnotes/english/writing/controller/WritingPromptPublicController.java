package com.starrainnotes.english.writing.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.writing.dto.WritingPromptDto.Prompt;
import com.starrainnotes.english.writing.dto.WritingPromptDto.Page;
import com.starrainnotes.english.writing.service.WritingPromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/english/content/writing-prompts")
public class WritingPromptPublicController {
    private final WritingPromptService service;

    @GetMapping
    public ApiResponse<Page> list(@RequestParam(required = false) String search,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(false, search, page, size));
    }

    @GetMapping("/{slug}")
    public ApiResponse<Prompt> get(@PathVariable String slug) {
        return ApiResponse.ok(service.get(slug, false));
    }
}
