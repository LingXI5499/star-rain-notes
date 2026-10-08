package com.starrainnotes.seo.renderer;

import com.starrainnotes.seo.dto.SeoPageModel;

import org.springframework.stereotype.Component;

@Component
public class SeoHtmlRenderer {
    public String render(SeoPageModel model) {
        String title = escape(model.getTitle());
        String description = escape(model.getDescription());
        String canonical = escape(model.getCanonicalUrl());
        String robots = escape(model.getRobotsDirective());
        String body = escape(plainText(model.getBodyMarkdown()));
        return "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
            + "<title>" + title + "</title><meta name=\"description\" content=\"" + description + "\">"
            + "<meta name=\"robots\" content=\"" + robots + "\">"
            + "<link rel=\"canonical\" href=\"" + canonical + "\">"
            + "<meta property=\"og:title\" content=\"" + title + "\">"
            + "<meta property=\"og:description\" content=\"" + description + "\">"
            + "<meta property=\"og:url\" content=\"" + canonical + "\">"
            + (model.getCoverUrl() == null ? "" : "<meta property=\"og:image\" content=\"" + escape(model.getCoverUrl()) + "\">")
            + structuredData(model) + "</head><body>"
            + "<div id=\"app\"><main><article><h1>" + title + "</h1>"
            + "<p>" + description + "</p><div style=\"white-space:pre-wrap\">" + body
            + "</div></article></main></div></body></html>";
    }

    private String structuredData(SeoPageModel model) {
        var data = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        data.put("@context", "https://schema.org").put("@type", "CreativeWork")
                .put("name", model.getTitle()).put("description", model.getDescription()).put("url", model.getCanonicalUrl());
        if (model.getCoverUrl() != null) { data.put("image", model.getCoverUrl()); }
        String json = data.toString().replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
        return "<script type=\"application/ld+json\">" + json + "</script>";
    }

    private String plainText(String markdown) {
        if (markdown == null) return "";
        String source = markdown.length() > 200_000 ? markdown.substring(0, 200_000) : markdown;
        return source.replaceAll("!\\[([^]]*)]\\([^)]*\\)", "$1")
            .replaceAll("\\[([^]]+)]\\([^)]*\\)", "$1")
            .replaceAll("<[^>]{0,500}>", " ")
            .replaceAll("(?m)^\\s{0,3}(?:#{1,6}|>|[-*+]|\\d+\\.)\\s+", "")
            .replaceAll("[`*_~]", "");
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
