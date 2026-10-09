package com.starrainnotes.english.knowledge.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.knowledge.dto.KnowledgeDto.Page;
import com.starrainnotes.english.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/public/english/knowledge/articles")
public class KnowledgeController {
    private final KnowledgeService service;
    @GetMapping public ApiResponse<Page> list(@RequestParam(required=false) String type,@RequestParam(required=false) String search,@RequestParam(required=false) Long topicId,@RequestParam(required=false) Long genreId,@RequestParam(required=false) Long purposeId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size) { return ApiResponse.ok(service.list(type,search,topicId,genreId,purposeId,page,size)); }
}
