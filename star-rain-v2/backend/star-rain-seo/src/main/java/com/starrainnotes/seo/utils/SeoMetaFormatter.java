package com.starrainnotes.seo.utils;

import com.starrainnotes.seo.dto.SeoSourceDocument;

public final class SeoMetaFormatter {
    private SeoMetaFormatter() {}

    public static String title(SeoSourceDocument source) {
        String title = source.getTitle() == null ? "" : source.getTitle().strip();
        if (title.isEmpty()) title = "星雨笔录";
        return title.equals("星雨笔录") ? title : truncate(title, 220) + " | 星雨笔录";
    }

    public static String description(SeoSourceDocument source) {
        String summary = source.getSummary() == null ? "" : source.getSummary().strip();
        if (summary.isEmpty()) summary = "星雨笔录：知识、代码与成长。";
        return truncate(summary.replaceAll("\\s+", " "), 300);
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
