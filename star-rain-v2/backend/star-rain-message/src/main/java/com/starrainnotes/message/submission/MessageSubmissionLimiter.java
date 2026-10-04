package com.starrainnotes.message.submission;

import com.starrainnotes.common.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MessageSubmissionLimiter {
    private static final int MAX_PER_MINUTE = 5;
    private static final long WINDOW_MILLIS = 60_000;
    private final byte[] secret = new byte[32];
    private final Map<String, Window> windows = new HashMap<>();

    public MessageSubmissionLimiter() {
        new SecureRandom().nextBytes(secret);
    }

    public synchronized void check(String remoteAddress) {
        long now = System.currentTimeMillis();
        if (windows.size() > 2048) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MILLIS);
        }
        if (windows.size() > 4096) {
            windows.clear();
        }
        String key = fingerprint(remoteAddress == null ? "unknown" : remoteAddress);
        Window window = windows.get(key);
        if (window == null || now - window.startedAt >= WINDOW_MILLIS) {
            windows.put(key, new Window(now, 1));
            return;
        }
        if (window.count >= MAX_PER_MINUTE) {
            throw new ApiException("MESSAGE_RATE_LIMITED", "留言过于频繁，请稍后再试", 429);
        }
        window.count++;
    }

    private String fingerprint(String address) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(secret);
            return Base64.getEncoder().encodeToString(digest.digest(address.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static class Window {
        private final long startedAt;
        private int count;

        private Window(long startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}
