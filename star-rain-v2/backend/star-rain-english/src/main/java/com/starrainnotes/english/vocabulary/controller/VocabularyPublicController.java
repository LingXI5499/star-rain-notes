package com.starrainnotes.english.vocabulary.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/english/vocabulary")
public class VocabularyPublicController {
    private final VocabularyService service;

    @GetMapping("/themes")
    public ApiResponse<List<VocabularyDto.Theme>> themes() { return ApiResponse.ok(service.themes()); }

    @GetMapping("/words")
    public ApiResponse<VocabularyDto.Page<VocabularyDto.Word>> words(
            @RequestParam(required = false) String themeId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(service.words(themeId, search, page, size));
    }

    @GetMapping("/words/{wordId}")
    public ApiResponse<VocabularyDto.Word> word(@PathVariable String wordId) {
        return ApiResponse.ok(service.word(wordId));
    }
}
