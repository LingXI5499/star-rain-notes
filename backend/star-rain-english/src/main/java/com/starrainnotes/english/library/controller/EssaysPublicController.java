package com.starrainnotes.english.library.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.library.dto.LibraryDto.Essay;
import com.starrainnotes.english.library.service.EnglishLibraryService;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Page;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.Article;
import com.starrainnotes.english.writing.article.service.WritingArticleService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Journal filters and facets use only the station author's eligible, published essays. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/english/essays")
public class EssaysPublicController {
    private final EnglishLibraryService library;
    private final WritingArticleService articles;
    @GetMapping
    public ApiResponse<Page<Essay>> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) String tag, @RequestParam(required = false) String month,
            @RequestParam(required = false) String genre, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(library.essays(q, tag, month, genre, page, size));
    }
    @GetMapping("/facets")
    public ApiResponse<Map<String, Object>> facets() { return ApiResponse.ok(library.essayFacets()); }
    @GetMapping("/{slug}")
    public ApiResponse<Article> get(@PathVariable String slug) { return ApiResponse.ok(articles.getPublic(slug)); }
}
