package com.starrainnotes.seo.controller;

import com.starrainnotes.seo.provider.impl.BlogSeoSourceProvider;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** V1 single-segment article paths preserve their slug and never redirect missing content to home. */
@RestController
public class LegacyBlogRedirectController {
    private final BlogSeoSourceProvider blogs;
    public LegacyBlogRedirectController(BlogSeoSourceProvider blogs) { this.blogs = blogs; }
    @GetMapping("/blog/{slug}")
    public ResponseEntity<Void> redirect(@PathVariable String slug) {
        if (!slug.matches("[a-z0-9][a-z0-9-]{0,179}")) return ResponseEntity.notFound().build();
        String route = "/blog/posts/" + slug;
        return blogs.loadByRoute(route).isPresent() ? ResponseEntity.status(301).location(URI.create(route)).build()
                : ResponseEntity.notFound().build();
    }
}
