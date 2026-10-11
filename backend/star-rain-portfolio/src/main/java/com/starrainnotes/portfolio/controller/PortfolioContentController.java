package com.starrainnotes.portfolio.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.portfolio.dto.IdOrderDTO;
import com.starrainnotes.portfolio.dto.WorkSectionDTO;
import com.starrainnotes.portfolio.service.PortfolioContentService;
import com.starrainnotes.portfolio.vo.*;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PortfolioContentController {
    private final PortfolioContentService content;
    @GetMapping("/api/public/portfolio/taxonomy")
    public ApiResponse<Map<String, List<WorkTaxonomyVO>>> taxonomy() { return ApiResponse.ok(content.taxonomy()); }
    @GetMapping("/api/admin/portfolio/templates")
    @PreAuthorize("hasAuthority('portfolio:read-admin')")
    public ApiResponse<List<WorkTemplateVO>> templates() { return ApiResponse.ok(content.templates()); }
    @PostMapping("/api/admin/portfolio/works/{workId}/sections")
    @PreAuthorize("hasAuthority('portfolio:edit')")
    public ApiResponse<WorkSectionVO> create(@PathVariable Long workId, @RequestBody WorkSectionDTO request) {
        return ApiResponse.ok(content.create(workId, request));
    }
    @PutMapping("/api/admin/portfolio/works/{workId}/sections/{sectionId}")
    @PreAuthorize("hasAuthority('portfolio:edit')")
    public ApiResponse<WorkSectionVO> update(@PathVariable Long workId, @PathVariable Long sectionId, @RequestBody WorkSectionDTO request) {
        return ApiResponse.ok(content.update(workId, sectionId, request));
    }
    @DeleteMapping("/api/admin/portfolio/works/{workId}/sections/{sectionId}")
    @PreAuthorize("hasAuthority('portfolio:edit')")
    public ApiResponse<Void> delete(@PathVariable Long workId, @PathVariable Long sectionId) {
        content.delete(workId, sectionId); return ApiResponse.ok(null);
    }
    @PutMapping("/api/admin/portfolio/works/{workId}/sections/order")
    @PreAuthorize("hasAuthority('portfolio:edit')")
    public ApiResponse<Void> order(@PathVariable Long workId, @RequestBody IdOrderDTO request) {
        content.reorder(workId, request.getIds()); return ApiResponse.ok(null);
    }
}
