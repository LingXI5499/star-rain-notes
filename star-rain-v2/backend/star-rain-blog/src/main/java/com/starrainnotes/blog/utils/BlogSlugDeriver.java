package com.starrainnotes.blog.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;

/* 名称生成公开地址：仅创建时调用，改名后沿用原地址。 */
public final class BlogSlugDeriver {

    private BlogSlugDeriver() {
    }

    public static String derive(String name, String prefix, int maxLength) {
        String ascii = name.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-+|-+$", "");
        String slug = BlogSlugRules.normalize(ascii);
        if (slug == null || slug.length() < 3) {
            // 纯中文或纯符号名称没有可读的 ASCII 词，稳定散列保证同名可复现。
            slug = prefix + "-" + shortHash(name);
        }
        return fit(slug, maxLength);
    }

    public static String withSuffix(String base, int ordinal, int maxLength) {
        if (ordinal < 1) {
            throw new IllegalArgumentException("ordinal must be positive");
        }
        if (ordinal == 1) {
            return base;
        }
        String suffix = "-" + ordinal;
        return fit(base, maxLength - suffix.length()) + suffix;
    }

    private static String fit(String value, int limit) {
        String trimmed = value.substring(0, Math.min(value.length(), limit)).replaceFirst("-+$", "");
        if (!BlogSlugRules.isValid(trimmed, limit)) {
            throw new IllegalArgumentException("derived slug is invalid");
        }
        return trimmed;
    }

    private static String shortHash(String name) {
        try {
            String normalized = Normalizer.normalize(name, Normalizer.Form.NFKC).strip();
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
