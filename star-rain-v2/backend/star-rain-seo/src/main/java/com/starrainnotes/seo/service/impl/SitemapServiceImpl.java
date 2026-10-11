package com.starrainnotes.seo.service.impl;

import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import com.starrainnotes.seo.service.SitemapService;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SitemapServiceImpl implements SitemapService {
    private final SeoPageMapper mapper;

    @Override
    public String xml() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        for (SeoPageSnapshot page : mapper.activePages()) {
            xml.append("<url><loc>").append(escape(page.getCanonicalUrl())).append("</loc>");
            // sourceVersionRef already persists the source timestamp; rebuilding is not an edit.
            if (page.getSourceVersionRef() != null) {
                try {
                    var modified = LocalDateTime.parse(page.getSourceVersionRef()).atOffset(ZoneOffset.UTC);
                    xml.append("<lastmod>").append(modified)
                        .append("</lastmod>");
                } catch (DateTimeParseException opaqueRevision) {
                    // Sources without a trustworthy date omit lastmod.
                }
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
