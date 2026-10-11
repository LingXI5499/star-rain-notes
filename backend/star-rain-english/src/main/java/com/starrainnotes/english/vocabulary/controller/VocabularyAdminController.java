package com.starrainnotes.english.vocabulary.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/english/vocabulary")
@PreAuthorize("hasAuthority('english:content-edit')")
public class VocabularyAdminController {
    private final VocabularyService service;

    @PostMapping("/themes")
    public ApiResponse<VocabularyDto.Theme> createTheme(@RequestBody VocabularyDto.ThemeRequest request) {
        return ApiResponse.ok(service.createTheme(request));
    }

    @PutMapping("/themes/{themeId}")
    public ApiResponse<VocabularyDto.Theme> updateTheme(@PathVariable String themeId,
                                                             @RequestBody VocabularyDto.ThemeRequest request) {
        return ApiResponse.ok(service.updateTheme(themeId, request));
    }

    @DeleteMapping("/themes/{themeId}")
    public ApiResponse<Void> deleteTheme(@PathVariable String themeId) {
        service.deleteTheme(themeId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/words")
    public ApiResponse<VocabularyDto.Word> createWord(@RequestBody VocabularyDto.WordRequest request) {
        return ApiResponse.ok(service.createWord(request));
    }

    @PutMapping("/words/{wordId}")
    public ApiResponse<VocabularyDto.Word> updateWord(@PathVariable String wordId,
                                                           @RequestBody VocabularyDto.WordRequest request) {
        return ApiResponse.ok(service.updateWord(wordId, request));
    }

    @DeleteMapping("/words/{wordId}")
    public ApiResponse<Void> deleteWord(@PathVariable String wordId) {
        service.deleteWord(wordId);
        return ApiResponse.ok(null);
    }
}
