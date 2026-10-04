package com.starrainnotes.seo.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.seo.api.SeoPageApi;
import com.starrainnotes.seo.render.SeoInteractiveAssets;
import com.starrainnotes.seo.robots.RobotsService;
import com.starrainnotes.seo.sitemap.SitemapService;
import com.starrainnotes.seo.snapshot.SeoPageSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SeoPublicController {
    private final SeoPageApi pages;
    private final SitemapService sitemap;
    private final RobotsService robots;
    private final SeoInteractiveAssets assets;

    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    public String sitemap() { return sitemap.xml(); }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() { return robots.text(); }

    @GetMapping("/api/public/seo/meta")
    public ResponseEntity<ApiResponse<Map<String, String>>> meta(@RequestParam String route) {
        return pages.getMetaByRoute(route)
            .map(page -> ResponseEntity.ok(ApiResponse.ok(Map.of(
                "title", page.getTitle(), "description", page.getDescription(),
                "canonicalUrl", page.getCanonicalUrl(), "robots", page.getRobotsDirective()))))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = {"/", "/blog", "/blog/archive", "/blog/posts/{slug}",
        "/tutorials", "/tutorials/{slug}", "/tutorials/{slug}/{chapterSlug}",
        "/portfolio", "/portfolio/{slug}", "/messages", "/about"},
        produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> html(HttpServletRequest request) {
        String path = request.getRequestURI();
        return pages.getByRoute(path).map(SeoPageSnapshot::getHtmlSnapshot)
            .map(assets::include)
            .map(body -> ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(body))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
