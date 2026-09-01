package com.starrainnotes.search.controller;

import com.starrainnotes.search.dto.SearchPageView;
import com.starrainnotes.search.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public global search (04 §15). Quick Search uses the same API with
 * pageSize=8.
 */
@RestController
@RequestMapping("/api/v1/public/search")
public class SearchPublicController {

    private final SearchService searchService;

    public SearchPublicController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchPageView search(@RequestParam String q,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int pageSize) {
        return searchService.search(q, type, page, pageSize);
    }
}
