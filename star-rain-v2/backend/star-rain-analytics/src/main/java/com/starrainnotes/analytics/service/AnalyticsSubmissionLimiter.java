package com.starrainnotes.analytics.service;

import com.starrainnotes.analytics.exception.AnalyticsEventRejectedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsSubmissionLimiter {
    private static final byte[] SALT = new byte[32];
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    static {
        new SecureRandom().nextBytes(SALT);
    }

    public void check(String address) {
        String key = hash(address);
        long minute = Instant.now().getEpochSecond() / 60;
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> entry.getValue().minute < minute);
        }
        Window window = windows.compute(key, (ignored, old) -> {
            if (old == null || old.minute != minute) return new Window(minute, 1);
            return new Window(minute, old.count + 1);
        });
        if (window.count > 60) throw new AnalyticsEventRejectedException();
    }

    private String hash(String address) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(SALT);
            return HexFormat.of().formatHex(digest.digest(address.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private record Window(long minute, int count) {}
}
