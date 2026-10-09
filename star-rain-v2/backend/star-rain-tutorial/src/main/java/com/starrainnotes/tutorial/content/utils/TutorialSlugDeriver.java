package com.starrainnotes.tutorial.content.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

/*
 * 教程、知识体系与章节的地址编号由服务端生成，作者只填写可读名称。
 * 对纯中文名称使用稳定哈希，避免非 ASCII 字符被规范化后得到空编号。
 */
public final class TutorialSlugDeriver {
    private TutorialSlugDeriver() {
    }

    public static String fromTitle(String title, String fallbackPrefix) {
        String normalized = Normalizer.normalize(title.strip(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (!normalized.isEmpty()) {
            return normalized.length() > 140 ? normalized.substring(0, 140).replaceAll("-+$", "") : normalized;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(title.strip().getBytes(StandardCharsets.UTF_8));
            return fallbackPrefix + "-" + HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException cause) {
            throw new IllegalStateException("JDK 缺少 SHA-256", cause);
        }
    }
}
