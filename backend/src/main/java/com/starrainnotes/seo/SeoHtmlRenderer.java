package com.starrainnotes.seo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SeoHtmlRenderer {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final SeoProperties properties;
    private final SeoContentRepository content;
    private final ObjectMapper json;
    private volatile CachedShell cachedShell;

    public SeoHtmlRenderer(SeoProperties properties, SeoContentRepository content, ObjectMapper json) {
        this.properties = properties;
        this.content = content;
        this.json = json;
    }

    public String render(SeoPage page) {
        String shell = shell();
        String siteName = content.site().name();
        String title = page.path().equals("/") ? page.title() : page.title() + " | " + siteName;
        String canonical = absolute(page.path());
        String image = absolute(page.imageUrl() == null ? properties.defaultShareImage() : page.imageUrl());

        StringBuilder head = new StringBuilder();
        meta(head, "name", "description", page.description());
        meta(head, "name", "robots", page.robots());
        meta(head, "property", "og:title", title);
        meta(head, "property", "og:description", page.description());
        meta(head, "property", "og:type", page.openGraphType());
        meta(head, "property", "og:url", canonical);
        meta(head, "property", "og:site_name", siteName);
        meta(head, "property", "og:image", image);
        meta(head, "name", "twitter:card", "summary_large_image");
        meta(head, "name", "twitter:title", title);
        meta(head, "name", "twitter:description", page.description());
        meta(head, "name", "twitter:image", image);
        if (page.publishedAt() != null) meta(head, "property", "article:published_time", format(page.publishedAt()));
        if (page.updatedAt() != null) meta(head, "property", "article:modified_time", format(page.updatedAt()));
        head.append("<link rel=\"canonical\" href=\"").append(attr(canonical)).append("\">\n");
        head.append("<script type=\"application/ld+json\" data-seo-schema>\n").append(schema(page, canonical, image)).append("\n</script>\n");

        String semantic = semanticBody(page);
        String cleaned = shell
                .replaceAll("(?is)<title>.*?</title>", "")
                .replaceAll("(?is)<meta\\s+(?:name|property)=\"(?:description|robots|og:[^\"]+|twitter:[^\"]+|article:[^\"]+)\"[^>]*>", "")
                .replaceAll("(?is)<link\\s+rel=\"canonical\"[^>]*>", "")
                .replace("</head>", "<title>" + text(title) + "</title>\n" + head + "</head>");
        if (cleaned.contains("<div id=\"app\"></div>")) return cleaned.replace("<div id=\"app\"></div>", "<div id=\"app\">" + semantic + "</div>");
        return cleaned.replace("<body>", "<body><div id=\"app\">" + semantic + "</div>");
    }

    public String renderNotFound(String path) {
        SeoPage page = new SeoPage(path, "页面不存在", "请求的页面不存在或尚未公开。", "noindex,nofollow", "website", "WebPage", null, null, null,
                "<p>你访问的内容不存在、已撤回或尚未公开。</p><p><a href=\"/\">返回首页</a></p>", List.of(), Map.of());
        return render(page);
    }

    private String semanticBody(SeoPage page) {
        StringBuilder body = new StringBuilder("<div class=\"seo-prerender\">");
        if (!page.breadcrumbs().isEmpty()) {
            body.append("<nav aria-label=\"面包屑\"><ol>");
            for (SeoBreadcrumb crumb : page.breadcrumbs()) body.append("<li><a href=\"").append(attr(crumb.path())).append("\">").append(text(crumb.name())).append("</a></li>");
            body.append("</ol></nav>");
        }
        body.append("<main><article><header><h1>").append(text(page.title())).append("</h1><p>").append(text(page.description())).append("</p></header>")
                .append(page.bodyHtml() == null ? "" : page.bodyHtml()).append("</article></main></div>");
        return body.toString();
    }

    private String schema(SeoPage page, String canonical, String image) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("@context", "https://schema.org");
        value.put("@type", page.schemaType());
        value.put("name", page.title());
        value.put("headline", page.title());
        value.put("description", page.description());
        value.put("url", canonical);
        value.put("mainEntityOfPage", canonical);
        value.put("image", image);
        if (page.publishedAt() != null) value.put("datePublished", format(page.publishedAt()));
        if (page.updatedAt() != null) value.put("dateModified", format(page.updatedAt()));
        value.putAll(page.schemaExtras());
        if ("Article".equals(page.schemaType())) {
            String author = content.authorName();
            value.put("author", Map.of("@type", "Person", "name", SeoProperties.blank(author) ? "零燨" : author));
        }
        if ("ProfilePage".equals(page.schemaType())) {
            Map<String, Object> person = new LinkedHashMap<>();
            person.put("@type", "Person");
            person.put("name", page.schemaExtras().getOrDefault("name", content.authorName()));
            if (page.schemaExtras().containsKey("sameAs")) person.put("sameAs", page.schemaExtras().get("sameAs"));
            value.put("mainEntity", person);
        }

        List<Object> graph = new ArrayList<>();
        graph.add(value);
        if (!page.breadcrumbs().isEmpty()) {
            List<Map<String, Object>> items = new ArrayList<>();
            int position = 1;
            for (SeoBreadcrumb crumb : page.breadcrumbs()) items.add(Map.of("@type", "ListItem", "position", position++, "name", crumb.name(), "item", absolute(crumb.path())));
            items.add(Map.of("@type", "ListItem", "position", position, "name", page.title(), "item", canonical));
            graph.add(Map.of("@type", "BreadcrumbList", "itemListElement", items));
        }
        try {
            return json.writeValueAsString(Map.of("@context", "https://schema.org", "@graph", graph));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot render SEO structured data", e);
        }
    }

    private String shell() {
        String configured = properties.frontendIndexPath();
        if (!SeoProperties.blank(configured)) {
            try {
                Path path = Path.of(configured);
                long modified = Files.getLastModifiedTime(path).toMillis();
                CachedShell current = cachedShell;
                if (current == null || current.modified() != modified) {
                    current = new CachedShell(modified, Files.readString(path, StandardCharsets.UTF_8));
                    cachedShell = current;
                }
                return current.html();
            } catch (IOException ignored) {
                // Local tests and development can use the classpath fallback.
            }
        }
        try (var input = new ClassPathResource("seo/spa-shell.html").getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No frontend index shell available", e);
        }
    }

    private String absolute(String value) {
        if (value == null || value.isBlank()) return properties.siteOrigin();
        if (value.startsWith("http://") || value.startsWith("https://")) return value;
        return properties.siteOrigin() + (value.startsWith("/") ? value : "/" + value);
    }
    private String format(java.time.LocalDateTime value) { return value.atOffset(ZoneOffset.UTC).format(ISO); }
    private void meta(StringBuilder out, String attr, String key, String value) { out.append("<meta ").append(attr).append("=\"").append(attr(key)).append("\" content=\"").append(attr(value)).append("\">\n"); }
    private String text(String value) { return HtmlUtils.htmlEscape(value == null ? "" : value); }
    private String attr(String value) { return HtmlUtils.htmlEscape(value == null ? "" : value); }
    private record CachedShell(long modified, String html) {}
}
