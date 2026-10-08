package com.starrainnotes.english.taxonomy.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import com.starrainnotes.english.taxonomy.service.TaxonomyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/public/english/taxonomy")
public class TaxonomyPublicController {
    private final TaxonomyService service;
    @GetMapping public ApiResponse<List<Node>> tree() { return ApiResponse.ok(service.tree(false)); }
    @GetMapping("/{slug}") public ApiResponse<Node> get(@PathVariable String slug) { return ApiResponse.ok(service.get(slug)); }
}
