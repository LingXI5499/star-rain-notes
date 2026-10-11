package com.starrainnotes.seo.renderer;

import com.starrainnotes.seo.dto.SeoPageModel;
import java.net.URI;
import java.util.List;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Heading;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SeoHtmlRenderer {
    private static final Logger log = LoggerFactory.getLogger(SeoHtmlRenderer.class);
    private static final int MAX_MARKDOWN_LENGTH = 200_000;
    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());
    private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();
    private final HtmlRenderer markdownRenderer = HtmlRenderer.builder().extensions(EXTENSIONS)
            .escapeHtml(true).sanitizeUrls(true).build();
    public String render(SeoPageModel model) {
        String title = escape(model.getTitle());
        String description = escape(model.getDescription());
        String canonical = escape(model.getCanonicalUrl());
        String robots = escape(model.getRobotsDirective());
        String body = semanticBody(model.getBodyMarkdown());
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
            + "<p>" + description + "</p><div class=\"seo-body\">" + body
            + "</div></article></main></div></body></html>";
    }

    private String structuredData(SeoPageModel model) {
        var data = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        String type = "BLOG".equals(model.getContentType()) ? "BlogPosting"
                : "CHAPTER".equals(model.getContentType()) ? "TechArticle" : "CreativeWork";
        data.put("@context", "https://schema.org").put("@type", type)
                .put("name", model.getTitle()).put("description", model.getDescription()).put("url", model.getCanonicalUrl());
        if (model.getCoverUrl() != null) { data.put("image", model.getCoverUrl()); }
        if (model.getUpdatedAt() != null) data.put("dateModified", model.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC).toString());
        String json = data.toString().replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
        return "<script type=\"application/ld+json\">" + json + "</script>";
    }

    private String semanticBody(String markdown) {
        if (markdown == null) return "";
        int end = Math.min(markdown.length(), MAX_MARKDOWN_LENGTH);
        if (end < markdown.length()) {
            if (Character.isHighSurrogate(markdown.charAt(end - 1))) end--;
            log.warn("SEO Markdown truncated from {} to {} characters", markdown.length(), end);
        }
        Node document = parser.parse(markdown.substring(0, end));
        document.accept(new AbstractVisitor() {
            @Override public void visit(Heading heading) {
                if (heading.getLevel() == 1) heading.setLevel(2);
                visitChildren(heading);
            }
            @Override public void visit(Link link) {
                if (!safeUrl(link.getDestination(), false)) link.setDestination("");
                visitChildren(link);
            }
            @Override public void visit(Image image) {
                if (!safeUrl(image.getDestination(), true)) image.setDestination("");
                visitChildren(image);
            }
        });
        return markdownRenderer.render(document);
    }

    private boolean safeUrl(String destination, boolean image) {
        if (destination == null || destination.isBlank() || destination.startsWith("//")
                || destination.indexOf('\\') >= 0 || destination.chars().anyMatch(Character::isISOControl)) return false;
        try {
            URI uri = URI.create(destination);
            if (uri.getUserInfo() != null) return false;
            String scheme = uri.getScheme();
            if (scheme == null) return !image || !destination.startsWith("#");
            return "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)
                    || (!image && "mailto".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
