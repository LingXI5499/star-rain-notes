package com.starrainnotes.seo.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.rebuild.SeoRebuildService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/seo")
@PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('seo:rebuild')")
public class SeoAdminController {
    private final SeoRebuildService rebuild;
    private final SeoPageMapper mapper;

    @PostMapping("/rebuild")
    public ApiResponse<Map<String, Object>> rebuild(@RequestParam(required = false) String route) {
        if (route == null || route.isBlank()) rebuild.rebuildAll();
        else rebuild.rebuildRoute(route);
        return ApiResponse.ok(Map.of("activePages", mapper.activeCount()));
    }
}
