package com.starrainnotes.english.taxonomy.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import com.starrainnotes.english.taxonomy.service.TaxonomyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/admin/english/taxonomy")
@PreAuthorize("hasAuthority('english:content-edit')")
public class TaxonomyAdminController {
    private final TaxonomyService service;
    @GetMapping public ApiResponse<List<Node>> tree() { return ApiResponse.ok(service.tree(true)); }
    @PostMapping public ApiResponse<Node> create(@RequestBody Node request) { return ApiResponse.ok(service.create(request)); }
    @PutMapping("/{id}") public ApiResponse<Node> update(@PathVariable long id,@RequestBody Node request) { return ApiResponse.ok(service.update(id,request)); }
    @PostMapping("/{id}/disable") public ApiResponse<Void> disable(@PathVariable long id) { service.disable(id); return ApiResponse.ok(null); }
}
