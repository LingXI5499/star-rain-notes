package com.starrainnotes.english.writing.article.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.*;
import com.starrainnotes.english.writing.article.service.WritingArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/public/english/content/writing-articles")
public class WritingArticlePublicController {
    private final WritingArticleService service;
    @GetMapping public ApiResponse<Page> list(@RequestParam(required=false) String search,@RequestParam(required=false) Long topicId,@RequestParam(required=false) Long genreId,@RequestParam(required=false) Long purposeId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size) { return ApiResponse.ok(service.list(false,search,null,topicId,genreId,purposeId,page,size)); }
    @GetMapping("/{slug}") public ApiResponse<Article> get(@PathVariable String slug) { return ApiResponse.ok(service.getPublic(slug)); }
}
