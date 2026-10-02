package com.starrainnotes.blog.utils;

import java.util.Locale;

/*
 * 文本规则与摘要派生。
 *
 * 摘要允许留空：作者写正文时往往不想再写一遍摘要，
 * 发布时若摘要为空就从句首派生一段，比强制填写更容易被坚持使用。
 */
public final class BlogTextRules {

    // 与 sr_blog_post.summary 的列长度一致，Service 先拒绝避免 MySQL 抛 500
    public static final int SUMMARY_MAX_LENGTH = 1000;
    public static final int TITLE_MAX_LENGTH = 255;

    // 派生摘要的长度：短到能进列表卡片，又足以说明文章在讲什么
    private static final int DERIVED_SUMMARY_LENGTH = 200;

    private BlogTextRules() {
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // 统一按去掉首尾空白后的长度判断，避免全空格标题绕过校验
    public static int length(String value) {
        return value == null ? 0 : value.trim().length();
    }

    /*
     * 从 Markdown 正文派生摘要。
     *
     * 去掉常见标记（标题井号、引用、列表符号、强调符号、代码围栏、链接语法）后取前 N 个字符：
     * 目的是给列表与 meta description 一段能读的文字，不是做完整的 Markdown 渲染。
     */
    public static String deriveSummary(String bodyMarkdown) {
        if (!hasText(bodyMarkdown)) {
            return null;
        }
        StringBuilder plain = new StringBuilder(bodyMarkdown.length());
        boolean inCodeFence = false;
        for (String rawLine : bodyMarkdown.split("\r?\n")) {
            String line = rawLine.trim();
            if (line.startsWith("```")) {
                inCodeFence = !inCodeFence;
                continue;
            }
            if (inCodeFence || line.isEmpty()) {
                continue;
            }
            String stripped = line
                    .replaceAll("^#{1,6}\\s*", "")
                    .replaceAll("^>\\s*", "")
                    .replaceAll("^[-*+]\\s+", "")
                    .replaceAll("^\\d+\\.\\s+", "")
                    .replaceAll("!\\[([^\\]]*)\\]\\(([^)]*)\\)", "$1")
                    .replaceAll("\\[([^\\]]*)\\]\\(([^)]*)\\)", "$1")
                    .replace("**", "")
                    .replace("__", "")
                    .replace("`", "")
                    .trim();
            if (stripped.isEmpty()) {
                continue;
            }
            if (plain.length() > 0) {
                plain.append(' ');
            }
            plain.append(stripped);
            if (plain.length() >= DERIVED_SUMMARY_LENGTH) {
                break;
            }
        }
        String result = plain.toString().trim();
        if (result.isEmpty()) {
            return null;
        }
        return result.length() <= DERIVED_SUMMARY_LENGTH
                ? result
                : result.substring(0, DERIVED_SUMMARY_LENGTH);
    }

    // 标签 / 专题名统一去掉首尾空白，避免 "Java" 与 "Java " 被当成两个不同名字
    public static String normalizeName(String value) {
        return hasText(value) ? value.trim() : null;
    }

    public static String normalizeDescription(String value) {
        return hasText(value) ? value.trim() : null;
    }

    public static String normalizeKeyword(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }
}
