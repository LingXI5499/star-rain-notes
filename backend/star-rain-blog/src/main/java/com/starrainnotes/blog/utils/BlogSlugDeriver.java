/*
 * BlogSlugDeriver：博客模块的工具文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 阅读入口：derive（推导博客固定链接标识Deriver）；withSuffix（包含Suffix处理入口）。
 */

package com.starrainnotes.blog.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;

/* 名称生成公开地址：仅创建时调用，改名后沿用原地址。 */
/**
 * 博客固定链接标识Deriver，属于博客模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class BlogSlugDeriver {

    /**
     * 构造BlogSlugDeriver，接收本类使用的依赖或初始化参数。
     */
    private BlogSlugDeriver() {
    }

    /**
     * 推导博客固定链接标识Deriver。
     * 关键处理步骤调用 <code>name.replaceAll</code>、<code>BlogSlugRules.normalize</code>、<code>slug.length</code>、<code>shortHash</code>。
     * 边界判断包含 <code>(slug == null || slug.length() &lt; 3)</code>。
     *
     * @param name 名称；类型为 <code>String</code>
     * @param prefix prefix；类型为 <code>String</code>
     * @param maxLength 最大Length；类型为 <code>int</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String derive(String name, String prefix, int maxLength) {
        String ascii = name.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-+|-+$", "");
        String slug = BlogSlugRules.normalize(ascii);
        if (slug == null || slug.length() < 3) {
            // 纯中文或纯符号名称没有可读的 ASCII 词，稳定散列保证同名可复现。
            slug = prefix + "-" + shortHash(name);
        }
        return fit(slug, maxLength);
    }

    /**
     * 包含Suffix处理入口。
     * 关键处理步骤调用 <code>fit</code>、<code>suffix.length</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(ordinal &lt; 1)</code>；<code>(ordinal == 1)</code>。
     *
     * @param base 基础；类型为 <code>String</code>
     * @param ordinal 序号；类型为 <code>int</code>
     * @param maxLength 最大Length；类型为 <code>int</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
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

    /**
     * fit处理入口。
     * 关键处理步骤调用 <code>value.substring</code>、<code>Math.min</code>、<code>value.length</code>、<code>BlogSlugRules.isValid</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(!BlogSlugRules.isValid(trimmed, limit))</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private static String fit(String value, int limit) {
        String trimmed = value.substring(0, Math.min(value.length(), limit)).replaceFirst("-+$", "");
        if (!BlogSlugRules.isValid(trimmed, limit)) {
            throw new IllegalArgumentException("derived slug is invalid");
        }
        return trimmed;
    }

    /**
     * short摘要处理入口。
     * 关键处理步骤调用 <code>Normalizer.normalize</code>、<code>MessageDigest.getInstance</code>、<code>normalized.getBytes</code>、<code>HexFormat.of</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param name 名称；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
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
