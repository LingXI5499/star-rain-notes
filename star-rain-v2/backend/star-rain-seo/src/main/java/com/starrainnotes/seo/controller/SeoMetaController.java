package com.starrainnotes.seo.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.seo.api.SeoPageApi;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SeoMetaController {
    private final SeoPageApi pages;

    @GetMapping("/api/public/seo/meta")
    public ResponseEntity<ApiResponse<Map<String, String>>> meta(@RequestParam String route) {
        return pages.getMetaByRoute(route)
            .map(page -> ResponseEntity.ok(ApiResponse.ok(Map.of(
                "title", page.getTitle(), "description", page.getDescription(),
                "canonicalUrl", page.getCanonicalUrl(), "robots", page.getRobotsDirective()))))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
