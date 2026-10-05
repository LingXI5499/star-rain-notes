package com.starrainnotes.english.writing.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.writing.dto.WritingPromptDto.Prompt;
import com.starrainnotes.english.writing.dto.WritingPromptDto.Page;
import com.starrainnotes.english.writing.dto.WritingPromptDto.Request;
import com.starrainnotes.english.writing.service.WritingPromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/english/content/writing-prompts")
@PreAuthorize("hasAuthority('english:content-read-admin')")
public class WritingPromptAdminController {
    private final WritingPromptService service;

    @GetMapping
    public ApiResponse<Page> list(@RequestParam(required = false) String search,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(true, search, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Prompt> get(@PathVariable String id) { return ApiResponse.ok(service.get(id, true)); }

    @PostMapping
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Prompt> create(@RequestBody Request request) { return ApiResponse.ok(service.create(request)); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Prompt> update(@PathVariable String id, @RequestBody Request request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @PostMapping("/{id}/{action:publish|withdraw}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Prompt> status(@PathVariable String id, @PathVariable String action) {
        return ApiResponse.ok(service.setPublished(id, "publish".equals(action)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
