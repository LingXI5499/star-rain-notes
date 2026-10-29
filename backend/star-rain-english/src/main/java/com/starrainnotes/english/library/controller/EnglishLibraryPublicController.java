package com.starrainnotes.english.library.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.library.service.EnglishLibraryService;
import com.starrainnotes.english.library.dto.LibraryDto.*;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Page;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Canonical library endpoints expose only public taxonomy and source vocabulary. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/english/library")
public class EnglishLibraryPublicController {
    private final EnglishLibraryService service;
    @GetMapping("/overview")
    public ApiResponse<Overview> overview() { return ApiResponse.ok(service.overview()); }
    @GetMapping("/{module}/taxonomy")
    public ApiResponse<List<Node>> tree(@PathVariable String module) { return ApiResponse.ok(service.tree(module)); }
    @GetMapping("/{module}/counts")
    public ApiResponse<Map<String, Long>> counts(@PathVariable String module) { return ApiResponse.ok(service.counts(module)); }
    @GetMapping("/vocabulary/words")
    public ApiResponse<Page<Word>> words(@RequestParam(required = false) String q,
            @RequestParam(required = false) String category, @RequestParam(required = false) String topic,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int size, @RequestParam(required = false) Long theme) {
        return ApiResponse.ok(service.words(q, category, topic, status, page, size, theme));
    }
}
