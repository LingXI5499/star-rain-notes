package com.starrainnotes.english.reading.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.reading.dto.ReadingDto.Article;
import com.starrainnotes.english.reading.dto.ReadingDto.Page;
import com.starrainnotes.english.reading.service.ReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/english/content/reading")
public class ReadingPublicController {
    private final ReadingService service;

    @GetMapping
    public ApiResponse<Page> list(@RequestParam(required = false) String search,
                                  @RequestParam(required=false) Long topicId, @RequestParam(required=false) Long genreId, @RequestParam(required=false) Long purposeId,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listFiltered(false, search, page, size, topicId, genreId, purposeId));
    }

    @GetMapping("/{slug}")
    public ApiResponse<Article> get(@PathVariable String slug) {
        return ApiResponse.ok(service.get(slug, false));
    }
}
