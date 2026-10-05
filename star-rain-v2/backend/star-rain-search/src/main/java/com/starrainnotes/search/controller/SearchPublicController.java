package com.starrainnotes.search.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.search.service.SearchRateLimitService;
import com.starrainnotes.search.service.SearchQueryService;
import com.starrainnotes.search.vo.SearchHitVO;
import com.starrainnotes.search.vo.SearchSuggestionVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/search")
public class SearchPublicController {
    private final SearchQueryService service;
    private final SearchRateLimitService rateLimit;

    @GetMapping
    public ApiResponse<PageResult<SearchHitVO>> search(@RequestParam(required = false) String q,
        @RequestParam(required = false) String type,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest http) {
        rateLimit.check(http.getRemoteAddr());
        return ApiResponse.ok(service.search(q, type, page, pageSize));
    }

    @GetMapping("/quick")
    public ApiResponse<List<SearchHitVO>> quick(@RequestParam(required = false) String q,
        @RequestParam(defaultValue = "8") int limit, HttpServletRequest http) {
        rateLimit.check(http.getRemoteAddr());
        return ApiResponse.ok(service.quick(q, limit));
    }

    @GetMapping("/suggestions")
    public ApiResponse<List<SearchSuggestionVO>> suggestions(@RequestParam(required = false) String q,
        @RequestParam(defaultValue = "8") int limit, HttpServletRequest http) {
        rateLimit.check(http.getRemoteAddr());
        return ApiResponse.ok(service.suggestions(q, limit));
    }

}
