package com.starrainnotes.portfolio.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class WorkSlugRules {
    private WorkSlugRules() {
    }

    public static String fromTitle(String title) {
        String normalized = title.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.length() >= 3) {
            return normalized.substring(0, Math.min(normalized.length(), 160)).replaceAll("-+$", "");
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(title.getBytes(StandardCharsets.UTF_8));
            return "work-" + HexFormat.of().formatHex(hash, 0, 6);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public static boolean valid(String slug) {
        return slug != null && slug.length() <= 180 && slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*");
    }
}
