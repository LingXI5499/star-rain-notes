package com.starrainnotes.seo;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@RestController
public class SeoController {
    private final SeoContentRepository content;
    private final SeoHtmlRenderer renderer;
    private final SeoSitemapService sitemap;
    private final SeoProperties properties;
    private final SeoDocumentCache cache;

    public SeoController(SeoContentRepository content, SeoHtmlRenderer renderer, SeoSitemapService sitemap, SeoProperties properties, SeoDocumentCache cache) {
        this.content = content;
        this.renderer = renderer;
        this.sitemap = sitemap;
        this.properties = properties;
        this.cache = cache;
    }

    @GetMapping(value = {"/", "/tutorials", "/tutorials/{*path}", "/blog", "/blog/{*path}", "/portfolio", "/portfolio/{*path}", "/english", "/english/{*path}", "/about", "/search"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(HttpServletRequest request) {
        String path = request.getRequestURI();
        SeoPage page = cache.page(path, () -> content.resolve(path));
        if (page == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(html()).header("X-Robots-Tag", "noindex, nofollow").body(renderer.renderNotFound(path));
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().contentType(html()).cacheControl(CacheControl.noCache());
        if (!page.indexable()) response.header("X-Robots-Tag", page.robots().replace(",", ", "));
        return response.body(renderer.render(page));
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic()).body(sitemap.sitemap());
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
                User-agent: *
                Allow: /
                Disallow: /admin/
                Disallow: /api/
                Disallow: /search
                Disallow: /preview/

                Sitemap: %s/sitemap.xml
                """.formatted(properties.siteOrigin());
    }

    @GetMapping(value = "/indexnow-key.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> indexNowKey() {
        if (!properties.indexNowEnabled() || SeoProperties.blank(properties.indexNowKey())) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "public, max-age=300").body(properties.indexNowKey());
    }

    private MediaType html() { return new MediaType("text", "html", StandardCharsets.UTF_8); }
}
