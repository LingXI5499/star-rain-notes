package com.starrainnotes.english.writing.article.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.Article;
import com.starrainnotes.english.knowledge.dto.RevisionDto.Request;
import com.starrainnotes.english.writing.article.service.WritingArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/admin/english/content/writing-articles")
@PreAuthorize("hasAuthority('english:content-publish')")
public class WritingArticleAdminController {
    private final WritingArticleService service;
    @PostMapping("/{id}/{action:publish|withdraw}") public ApiResponse<Article> publish(@PathVariable String id,@PathVariable String action,@RequestBody Request request) { return ApiResponse.ok(service.publish(id,request.getRowVersion(),"publish".equals(action))); }
}
