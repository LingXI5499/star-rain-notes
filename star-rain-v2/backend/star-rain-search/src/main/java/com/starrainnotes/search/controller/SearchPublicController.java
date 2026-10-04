package com.starrainnotes.search.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.search.exception.SearchRateLimitedException;
import com.starrainnotes.search.query.SearchQueryService;
import com.starrainnotes.search.vo.SearchHitVO;
import com.starrainnotes.search.vo.SearchSuggestionVO;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/search")
public class SearchPublicController {
    private static final byte[] SALT = new byte[32];
    private static final ConcurrentHashMap<String, Window> WINDOWS = new ConcurrentHashMap<>();
    static { new SecureRandom().nextBytes(SALT); }
    private final SearchQueryService service;

    @GetMapping
    public ApiResponse<PageResult<SearchHitVO>> search(@RequestParam(required = false) String q,
        @RequestParam(required = false) String type,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest http) {
        checkRate(http);
        return ApiResponse.ok(service.search(q, type, page, pageSize));
    }

    @GetMapping("/quick")
    public ApiResponse<List<SearchHitVO>> quick(@RequestParam(required = false) String q,
        @RequestParam(defaultValue = "8") int limit, HttpServletRequest http) {
        checkRate(http);
        return ApiResponse.ok(service.quick(q, limit));
    }

    @GetMapping("/suggestions")
    public ApiResponse<List<SearchSuggestionVO>> suggestions(@RequestParam(required = false) String q,
        @RequestParam(defaultValue = "8") int limit, HttpServletRequest http) {
        checkRate(http);
        return ApiResponse.ok(service.suggestions(q, limit));
    }

    private void checkRate(HttpServletRequest http) {
        String key;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(SALT);
            key = HexFormat.of().formatHex(digest.digest(http.getRemoteAddr().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        long minute = Instant.now().getEpochSecond() / 60;
        if (WINDOWS.size() > 10_000) WINDOWS.entrySet().removeIf(entry -> entry.getValue().minute < minute);
        Window window = WINDOWS.compute(key, (ignored, old) ->
            old == null || old.minute != minute ? new Window(minute, 1) : new Window(minute, old.count + 1));
        if (window.count > 60) throw new SearchRateLimitedException();
    }

    private static final class Window {
        private final long minute;
        private final int count;
        private Window(long minute, int count) { this.minute = minute; this.count = count; }
    }
}
