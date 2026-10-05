package com.starrainnotes.search.service;

import com.starrainnotes.search.exception.SearchRateLimitedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

@Service
public class SearchRateLimitService {
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private final byte[] salt = new byte[32];
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public SearchRateLimitService() { new SecureRandom().nextBytes(salt); }

    public void check(String remoteAddress) {
        String key = hash(remoteAddress);
        long minute = Instant.now().getEpochSecond() / 60;
        if (windows.size() > 10_000) windows.entrySet().removeIf(entry -> entry.getValue().minute < minute);
        Window window = windows.compute(key, (ignored, old) ->
                old == null || old.minute != minute ? new Window(minute, 1) : new Window(minute, old.count + 1));
        if (window.count > MAX_REQUESTS_PER_MINUTE) throw new SearchRateLimitedException();
    }

    private String hash(String address) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return HexFormat.of().formatHex(digest.digest(address.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Window {
        private long minute;
        private int count;
    }
}
