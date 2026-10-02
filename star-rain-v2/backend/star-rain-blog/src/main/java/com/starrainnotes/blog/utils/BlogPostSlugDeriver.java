package com.starrainnotes.blog.utils;

import com.starrainnotes.blog.constant.BlogLimits;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;

/* 标题派生的 slug 只在创建时确定；之后改标题不改已有公开地址。 */
public final class BlogPostSlugDeriver {

    private BlogPostSlugDeriver() {
    }

    public static String derive(String title) {
        String ascii = title.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-+|-+$", "");
        String slug = BlogSlugRules.normalize(ascii);
        if (slug == null || slug.length() < 3) {
            // 中文或纯符号标题没有可用的 ASCII 词；标题哈希让回退值可复现。
            slug = "post-" + shortHash(title);
        }
        return fit(slug, BlogLimits.POST_SLUG_MAX_LENGTH);
    }

    public static String withSuffix(String base, int ordinal) {
        if (ordinal < 1) {
            throw new IllegalArgumentException("ordinal must be positive");
        }
        if (ordinal == 1) {
            return base;
        }
        String suffix = "-" + ordinal;
        return fit(base, BlogLimits.POST_SLUG_MAX_LENGTH - suffix.length()) + suffix;
    }

    private static String fit(String value, int limit) {
        String trimmed = value.substring(0, Math.min(value.length(), limit)).replaceFirst("-+$", "");
        if (!BlogSlugRules.isValid(trimmed, limit)) {
            throw new IllegalArgumentException("derived slug is invalid");
        }
        return trimmed;
    }

    private static String shortHash(String title) {
        try {
            String normalized = Normalizer.normalize(title, Normalizer.Form.NFKC).strip();
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
