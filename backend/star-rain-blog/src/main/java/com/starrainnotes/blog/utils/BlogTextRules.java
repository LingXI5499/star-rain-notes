/*
 * BlogTextRules：博客模块的工具文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 阅读入口：hasText（判断是否具备文本）；length（length处理入口）；deriveSummary（推导摘要）；publicSummary（公开摘要处理入口）。
 */

package com.starrainnotes.blog.utils;

import java.util.Locale;
import java.util.regex.Pattern;

/*
 * 文本规则与摘要派生。
 *
 * 摘要允许留空：作者写正文时往往不想再写一遍摘要，
 * 发布时若摘要为空就从句首派生一段，比强制填写更容易被坚持使用。
 */
/**
 * 博客文本规则，属于博客模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class BlogTextRules {

    // 与 sr_blog_post.summary 的列长度一致，Service 先拒绝避免 MySQL 抛 500
    /**
     * LENGTH。
     * 初始值为 <code>1000</code>。
     */
    public static final int SUMMARY_MAX_LENGTH = 1000;
    /**
     * LENGTH。
     * 初始值为 <code>255</code>。
     */
    public static final int TITLE_MAX_LENGTH = 255;

    // 派生摘要的长度：短到能进列表卡片，又足以说明文章在讲什么
    /**
     * LENGTH。
     * 初始值为 <code>200</code>。
     */
    private static final int DERIVED_SUMMARY_LENGTH = 200;
    /**
     * MATH。
     */
    private static final Pattern INLINE_DOLLAR_MATH = Pattern.compile("\\$[^$\\n]+\\$");

    /**
     * 构造BlogTextRules，接收本类使用的依赖或初始化参数。
     */
    private BlogTextRules() {
    }

    /**
     * 判断是否具备文本。
     * 关键处理步骤调用 <code>value.isBlank</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // 统一按去掉首尾空白后的长度判断，避免全空格标题绕过校验
    /**
     * length处理入口。
     * 关键处理步骤调用 <code>value.trim</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    public static int length(String value) {
        return value == null ? 0 : value.trim().length();
    }

    /*
     * 从 Markdown 正文派生摘要。
     *
     * 去掉常见标记（标题井号、引用、列表符号、强调符号、代码围栏、链接语法）后取前 N 个字符：
     * 目的是给列表与 meta description 一段能读的文字，不是做完整的 Markdown 渲染。
     */
    /**
     * 推导摘要。
     * 关键处理步骤调用 <code>hasText</code>、<code>hasMathMarkup</code>、<code>deriveMathSafeSummary</code>、<code>bodyMarkdown.length</code>。
     * 边界判断包含 <code>(!hasText(bodyMarkdown))</code>；<code>(hasMathMarkup(bodyMarkdown))</code>。
     *
     * @param bodyMarkdown Markdown 格式的正文；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
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

    /**
     * 判断是否具备MathMarkup。
     * 主要协作调用：<code>INLINE_DOLLAR_MATH.matcher</code>。
     *
     * @param text 文本；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    public static boolean hasMathMarkup(String text) {
        return text != null && (text.contains("\\(") || text.contains("\\[")
                || text.contains("$$") || text.contains("\\begin{")
                || INLINE_DOLLAR_MATH.matcher(text).find());
    }

    /**
     * 推导Math安全摘要。
     * 关键处理步骤调用 <code>hasText</code>、<code>bodyMarkdown.split</code>、<code>rawLine.trim</code>、<code>line.startsWith</code>。
     * 边界判断包含 <code>(!hasText(bodyMarkdown))</code>；<code>(line.startsWith("```"))</code>。
     *
     * @param bodyMarkdown Markdown 格式的正文；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
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
    /**
     * 规范化名称。
     * 关键处理步骤调用 <code>hasText</code>、<code>value.trim</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String normalizeName(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 规范化描述。
     * 关键处理步骤调用 <code>hasText</code>、<code>value.trim</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String normalizeDescription(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 规范化关键词。
     * 关键处理步骤调用 <code>hasText</code>、<code>value.trim</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String normalizeKeyword(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }
}
