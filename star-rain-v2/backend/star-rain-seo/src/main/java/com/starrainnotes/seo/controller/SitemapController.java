package com.starrainnotes.seo.controller;

import com.starrainnotes.seo.service.SitemapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SitemapController {
    private final SitemapService service;

    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    public String sitemap() { return service.xml(); }
}
