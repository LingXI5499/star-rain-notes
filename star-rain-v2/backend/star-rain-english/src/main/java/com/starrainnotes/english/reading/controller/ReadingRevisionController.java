package com.starrainnotes.english.reading.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.knowledge.dto.RevisionDto.*;
import com.starrainnotes.english.reading.dto.ReadingDto.Article;
import com.starrainnotes.english.reading.service.ReadingRevisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/admin/english/content/reading/{id}/revisions")
@PreAuthorize("hasAuthority('english:content-read-admin')")
public class ReadingRevisionController {
    private final ReadingRevisionService service;
    @GetMapping public ApiResponse<Page> list(@PathVariable String id,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size) { return ApiResponse.ok(service.list(id,page,size)); }
    @GetMapping("/{no}") public ApiResponse<Revision> get(@PathVariable String id,@PathVariable long no) { return ApiResponse.ok(service.get(id,no)); }
    @PostMapping @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Revision> snapshot(@PathVariable String id,@RequestBody Request request) { return ApiResponse.ok(service.snapshot(id,request)); }
    @PostMapping("/{no}/restore") @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Article> restore(@PathVariable String id,@PathVariable long no,@RequestBody Request request) { return ApiResponse.ok(service.restore(id,no,request)); }
}
