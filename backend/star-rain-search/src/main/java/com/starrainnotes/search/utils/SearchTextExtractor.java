/*
 * SearchTextExtractor：搜索模块的工具文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 阅读入口：plainText（纯文本文本处理入口）。
 */

package com.starrainnotes.search.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 搜索文本Extractor，属于搜索模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
@Component
public class SearchTextExtractor {
    /**
     * FENCE。
     */
    private static final Pattern CODE_FENCE = Pattern.compile("(?s)```[^\\n]*\\n(.*?)```");

    /**
     * 纯文本文本处理入口。
     * 主要协作调用：<code>CODE_FENCE.matcher</code>。
     * 边界判断包含 <code>(markdown == null || markdown.isBlank())</code>。
     *
     * @param markdown Markdown；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String plainText(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        String source = markdown.length() > 200_000 ? markdown.substring(0, 200_000) : markdown;
        Matcher code = CODE_FENCE.matcher(source);
        StringBuilder limited = new StringBuilder();
        while (code.find()) {
            String body = code.group(1);
            code.appendReplacement(limited, Matcher.quoteReplacement(body.substring(0, Math.min(body.length(), 2_000))));
        }
        code.appendTail(limited);
        String text = limited.toString()
            .replaceAll("!\\[([^]]*)]\\([^)]*\\)", "$1")
            .replaceAll("\\[([^]]+)]\\([^)]*\\)", "$1")
            .replaceAll("<[^>]{0,500}>", " ")
            .replaceAll("(?m)^\\s{0,3}(?:#{1,6}|>|[-*+]|\\d+\\.)\\s+", " ")
            .replaceAll("[`*_~]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return text.length() > 100_000 ? text.substring(0, 100_000) : text;
    }
}
