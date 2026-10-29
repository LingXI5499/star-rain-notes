package com.starrainnotes.english.library.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.knowledge.dto.RevisionDto;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.*;
import com.starrainnotes.english.writing.article.service.WritingArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Station essays use the existing owner-scoped drafts, revisions and publication workflow. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/english/essays")
@PreAuthorize("hasAuthority('english:content-edit')")
public class EssaysAdminController {
    private final WritingArticleService service;

    @GetMapping
    public ApiResponse<Page> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(true, search, state, null, null, null, page, size));
    }
    @GetMapping("/{id}")
    public ApiResponse<Article> get(@PathVariable String id) { return ApiResponse.ok(service.get(id)); }
    @PostMapping
    public ApiResponse<Article> create(@RequestBody Request value) { return ApiResponse.ok(service.create(value)); }
    @PutMapping("/{id}")
    public ApiResponse<Article> save(@PathVariable String id, @RequestBody Request value) { return ApiResponse.ok(service.save(id, value)); }
    @PostMapping("/{id}/complete")
    public ApiResponse<Article> complete(@PathVariable String id, @RequestBody RevisionDto.Request value) {
        return ApiResponse.ok(service.complete(id, value.getRowVersion()));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) { service.delete(id); return ApiResponse.ok(null); }
}
