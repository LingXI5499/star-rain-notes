package com.starrainnotes.site.section;

import com.starrainnotes.common.result.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/site/home-sections")
@PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('site:section-manage')")
public class HomeSectionAdminController {
    private final HomeSectionService service;

    @GetMapping
    public ApiResponse<List<HomeSectionEntity>> all() { return ApiResponse.ok(service.all()); }

    @PutMapping("/order")
    public ApiResponse<List<HomeSectionEntity>> order(@RequestBody HomeSectionOrder request) {
        return ApiResponse.ok(service.reorder(request));
    }

    @PatchMapping("/{sectionCode}")
    public ApiResponse<HomeSectionEntity> patch(@PathVariable String sectionCode, @RequestBody HomeSectionPatch request) {
        return ApiResponse.ok(service.patch(sectionCode, request));
    }
}
