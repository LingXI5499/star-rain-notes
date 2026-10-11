package com.starrainnotes.seo.controller;

import com.starrainnotes.seo.service.RobotsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RobotsController {
    private final RobotsService service;

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() { return service.robotsTxt(); }
}
