/*
 * WorkSlugRules：作品模块的工具文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 阅读入口：fromTitle（从输入构造标题）；valid（有效处理入口）。
 */

package com.starrainnotes.portfolio.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 作品固定链接标识规则，属于作品模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class WorkSlugRules {
    /**
     * 构造WorkSlugRules，接收本类使用的依赖或初始化参数。
     */
    private WorkSlugRules() {
    }

    /**
     * 从输入构造标题。
     * 关键处理步骤调用 <code>title.toLowerCase</code>、<code>normalized.length</code>、<code>normalized.substring</code>、<code>Math.min</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(normalized.length() &gt;= 3)</code>。
     *
     * @param title 标题；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
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

    /**
     * 有效处理入口。
     * 关键处理步骤调用 <code>slug.length</code>、<code>slug.matches</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    public static boolean valid(String slug) {
        return slug != null && slug.length() <= 180 && slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*");
    }
}
