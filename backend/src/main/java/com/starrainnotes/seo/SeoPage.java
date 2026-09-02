package com.starrainnotes.seo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record SeoPage(
        String path,
        String title,
        String description,
        String robots,
        String openGraphType,
        String schemaType,
        String imageUrl,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        String bodyHtml,
        List<SeoBreadcrumb> breadcrumbs,
        Map<String, Object> schemaExtras) {

    public SeoPage {
        robots = robots == null ? "index,follow" : robots;
        openGraphType = openGraphType == null ? "website" : openGraphType;
        breadcrumbs = breadcrumbs == null ? List.of() : List.copyOf(breadcrumbs);
        schemaExtras = schemaExtras == null ? Map.of() : Map.copyOf(schemaExtras);
    }

    public boolean indexable() {
        return robots.startsWith("index,");
    }
}
