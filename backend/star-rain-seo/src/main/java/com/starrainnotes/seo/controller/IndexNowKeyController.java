package com.starrainnotes.seo.controller;

import com.starrainnotes.seo.config.SeoProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** IndexNow keyLocation avoids depending on an untracked verification file in dist. */
@RestController
public class IndexNowKeyController {
    private final SeoProperties properties;
    public IndexNowKeyController(SeoProperties properties) { this.properties = properties; }
    @GetMapping(value = "/api/public/seo/indexnow-key.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> key() {
        String key = properties.getIndexnow().getKey();
        if (!properties.getIndexnow().isEnabled() || key == null || !key.matches("[A-Za-z0-9-]{8,128}")) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(key);
    }
}
