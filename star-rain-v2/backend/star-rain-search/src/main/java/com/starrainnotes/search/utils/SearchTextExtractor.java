package com.starrainnotes.search.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SearchTextExtractor {
    private static final Pattern CODE_FENCE = Pattern.compile("(?s)```[^\\n]*\\n(.*?)```");

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
