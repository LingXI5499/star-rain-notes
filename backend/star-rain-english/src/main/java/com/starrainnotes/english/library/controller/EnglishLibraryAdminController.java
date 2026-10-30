package com.starrainnotes.english.library.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.library.dto.LibraryDto.Classification;
import com.starrainnotes.english.library.service.EnglishLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Editorial writes retain the application's existing session, CSRF and content permission checks. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/english/library/vocabulary")
@PreAuthorize("hasAuthority('english:content-edit')")
public class EnglishLibraryAdminController {
    private final EnglishLibraryService service;
    @PostMapping
    public ApiResponse<com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word> create(@RequestBody com.starrainnotes.english.library.dto.LibraryDto.NewWord value) {
        return ApiResponse.ok(service.createWord(value));
    }
    @PutMapping("/{id}")
    public ApiResponse<Void> classify(@PathVariable long id, @RequestBody Classification value) {
        service.classify(id, value);
        return ApiResponse.ok(null);
    }
}
