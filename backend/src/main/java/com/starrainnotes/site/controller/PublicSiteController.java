package com.starrainnotes.site.controller;

import com.starrainnotes.site.dto.PublicHomeView;
import com.starrainnotes.site.dto.PublicSiteView;
import com.starrainnotes.site.service.SiteQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public site foundation (04 §8): site config and home data.
 * Anonymous access; never exposes internal media storage paths.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicSiteController {

    private final SiteQueryService queryService;

    @GetMapping("/site")
    public PublicSiteView site() {
        return queryService.getPublicSite();
    }

    @GetMapping("/home")
    public PublicHomeView home() {
        return queryService.getHome();
    }
}
