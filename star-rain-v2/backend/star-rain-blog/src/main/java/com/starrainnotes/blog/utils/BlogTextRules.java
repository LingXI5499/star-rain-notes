package com.starrainnotes.blog.utils;

import java.util.Locale;
import java.util.regex.Pattern;

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
    private static final Pattern INLINE_DOLLAR_MATH = Pattern.compile("\\$[^$\\n]+\\$");

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
        if (hasMathMarkup(bodyMarkdown)) {
            return deriveMathSafeSummary(bodyMarkdown);
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

    /**
     * 已有文章可能把截断的 TeX 保存进摘要；公开展示时用正文重新提取可读文字。
     * 手写的普通文字摘要原样保留。
     */
    public static String publicSummary(String summary, String bodyMarkdown) {
        return hasMathMarkup(summary) ? deriveMathSafeSummary(bodyMarkdown) : summary;
    }

    private static boolean hasMathMarkup(String text) {
        return text != null && (text.contains("\\(") || text.contains("\\[")
                || text.contains("$$") || text.contains("\\begin{")
                || INLINE_DOLLAR_MATH.matcher(text).find());
    }

    private static String deriveMathSafeSummary(String bodyMarkdown) {
        if (!hasText(bodyMarkdown)) {
            return null;
        }
        StringBuilder plain = new StringBuilder(DERIVED_SUMMARY_LENGTH);
        boolean inCodeFence = false;
        boolean inMathBlock = false;
        for (String rawLine : bodyMarkdown.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.startsWith("```")) {
                inCodeFence = !inCodeFence;
                continue;
            }
            if (inCodeFence || line.isEmpty()) {
                continue;
            }
            if (inMathBlock) {
                if (line.contains("\\]") || line.contains("$$") || line.contains("\\end{")) {
                    inMathBlock = false;
                }
                continue;
            }
            if (line.startsWith("\\[") || line.startsWith("$$") || line.startsWith("\\begin{")) {
                if (!line.contains("\\]") && !line.endsWith("$$") && !line.contains("\\end{")) {
                    inMathBlock = true;
                }
                continue;
            }
            if (line.startsWith("#") || hasMathMarkup(line)) {
                continue;
            }
            String stripped = line
                    .replaceAll("^>\\s*", "")
                    .replaceAll("^[-*+]\\s+", "")
                    .replaceAll("^\\d+\\.\\s+", "")
                    .replaceAll("!\\[([^\\]]*)\\]\\(([^)]*)\\)", "$1")
                    .replaceAll("\\[([^\\]]*)\\]\\(([^)]*)\\)", "$1")
                    .replace("**", "")
                    .replace("__", "")
                    .replace("`", "")
                    .trim();
            if (stripped.isEmpty() || stripped.endsWith(":") || stripped.endsWith("：")) {
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
        if (plain.isEmpty()) {
            return null;
        }
        if (plain.length() <= DERIVED_SUMMARY_LENGTH) {
            return plain.toString();
        }
        String excerpt = plain.substring(0, DERIVED_SUMMARY_LENGTH);
        int sentenceEnd = Math.max(excerpt.lastIndexOf('。'),
                Math.max(excerpt.lastIndexOf('！'), excerpt.lastIndexOf('？')));
        return sentenceEnd >= 80 ? excerpt.substring(0, sentenceEnd + 1) : excerpt;
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
