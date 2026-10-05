package com.starrainnotes.seo.service;

import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SitemapService {
    private final SeoPageMapper mapper;

    public String xml() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        for (SeoPageSnapshot page : mapper.activePages()) {
            xml.append("<url><loc>").append(escape(page.getCanonicalUrl())).append("</loc>");
            if (page.getGeneratedAt() != null) {
                xml.append("<lastmod>").append(page.getGeneratedAt().atOffset(ZoneOffset.UTC))
                    .append("</lastmod>");
            }
            xml.append("</url>");
        }
        return xml.append("</urlset>").toString();
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
