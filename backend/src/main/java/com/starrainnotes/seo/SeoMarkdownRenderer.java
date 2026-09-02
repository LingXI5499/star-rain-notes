package com.starrainnotes.seo;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

@Component
public class SeoMarkdownRenderer {
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build();

    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        return renderer.render(parser.parse(markdown));
    }
}
