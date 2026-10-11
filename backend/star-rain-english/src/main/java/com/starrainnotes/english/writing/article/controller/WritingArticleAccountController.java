package com.starrainnotes.english.writing.article.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.*;
import com.starrainnotes.english.knowledge.dto.RevisionDto;
import com.starrainnotes.english.writing.article.service.WritingArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/account/english/writing/articles")
public class WritingArticleAccountController {
    private final WritingArticleService service;
    @GetMapping public ApiResponse<Page> list(@RequestParam(required=false) String search,@RequestParam(required=false) String state,@RequestParam(required=false) Long topicId,@RequestParam(required=false) Long genreId,@RequestParam(required=false) Long purposeId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size) { return ApiResponse.ok(service.list(true,search,state,topicId,genreId,purposeId,page,size)); }
    @GetMapping("/{id}") public ApiResponse<Article> get(@PathVariable String id) { return ApiResponse.ok(service.get(id)); }
    @PostMapping public ApiResponse<Article> create(@RequestBody Request r) { return ApiResponse.ok(service.create(r)); }
    @PutMapping("/{id}") public ApiResponse<Article> save(@PathVariable String id,@RequestBody Request r) { return ApiResponse.ok(service.save(id,r)); }
    @PostMapping("/{id}/complete") public ApiResponse<Article> complete(@PathVariable String id,@RequestBody RevisionDto.Request r) { return ApiResponse.ok(service.complete(id,r.getRowVersion())); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable String id) { service.delete(id);return ApiResponse.ok(null); }
    @GetMapping("/{id}/revisions") public ApiResponse<RevisionDto.Page> revisions(@PathVariable String id,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size) { return ApiResponse.ok(service.revisions(id,page,size)); }
    @GetMapping("/{id}/revisions/{no}") public ApiResponse<RevisionDto.Revision> revision(@PathVariable String id,@PathVariable long no) { return ApiResponse.ok(service.revision(id,no)); }
    @PostMapping("/{id}/revisions") public ApiResponse<RevisionDto.Revision> snapshot(@PathVariable String id,@RequestBody RevisionDto.Request r) { return ApiResponse.ok(service.snapshot(id,r)); }
    @PostMapping("/{id}/revisions/{no}/restore") public ApiResponse<Article> restore(@PathVariable String id,@PathVariable long no,@RequestBody RevisionDto.Request r) { return ApiResponse.ok(service.restore(id,no,r)); }
}
