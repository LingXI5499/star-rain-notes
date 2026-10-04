package com.starrainnotes.analytics.controller;

import com.starrainnotes.analytics.dto.PageViewDTO;
import com.starrainnotes.analytics.exception.AnalyticsEventRejectedException;
import com.starrainnotes.analytics.exception.AnalyticsRouteInvalidException;
import com.starrainnotes.analytics.service.AnalyticsService;
import com.starrainnotes.common.result.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/analytics")
public class AnalyticsPublicController {
    private static final byte[] SALT = new byte[32];
    private static final ConcurrentHashMap<String, Window> WINDOWS = new ConcurrentHashMap<>();
    static { new SecureRandom().nextBytes(SALT); }
    private final AnalyticsService service;

    @PostMapping("/page-view")
    public ApiResponse<Void> pageView(@RequestBody PageViewDTO request, HttpServletRequest http) {
        if (request == null) throw new AnalyticsRouteInvalidException();
        if (!allow(http.getRemoteAddr())) throw new AnalyticsEventRejectedException();
        service.recordPageView(request.getRouteKey(), request.getReferrer());
        return ApiResponse.ok(null);
    }

    private boolean allow(String address) {
        String key;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(SALT);
            key = HexFormat.of().formatHex(digest.digest(address.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        long minute = Instant.now().getEpochSecond() / 60;
        if (WINDOWS.size() > 10000) WINDOWS.entrySet().removeIf(entry -> entry.getValue().minute < minute);
        Window window = WINDOWS.compute(key, (ignored, old) -> {
            if (old == null || old.minute != minute) return new Window(minute, 1);
            return new Window(minute, old.count + 1);
        });
        return window.count <= 60;
    }

    private static final class Window {
        private final long minute;
        private final int count;
        private Window(long minute, int count) { this.minute = minute; this.count = count; }
    }
}
