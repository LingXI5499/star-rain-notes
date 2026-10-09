package com.starrainnotes.seo.controller;

import com.starrainnotes.seo.service.SeoHtmlPageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SeoHtmlController {
    private final SeoHtmlPageService service;

    @GetMapping(value = {"/", "/blog", "/blog/archive", "/blog/posts/{slug}", "/blog/topics/{slug}",
        "/tutorials", "/tutorials/{slug}", "/tutorials/{slug}/{chapterSlug}",
        "/portfolio", "/portfolio/{slug}", "/messages", "/about"},
        produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> html(HttpServletRequest request) {
        return service.html(request.getRequestURI())
            .map(body -> ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(body))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
