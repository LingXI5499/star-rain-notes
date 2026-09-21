package com.starrainnotes.seo;

import com.starrainnotes.seo.repository.SeoSitemapRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SeoSitemapService {
    private static final long CACHE_MILLIS = 300_000L;
    private final SeoSitemapRepository repository;
    private final SeoProperties properties;
    private final BoundedSeoCache<String> cache = new BoundedSeoCache<>(1, CACHE_MILLIS, System::currentTimeMillis);

    public SeoSitemapService(SeoSitemapRepository repository, SeoProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public String sitemap() {
        return cache.get("sitemap", this::buildSitemap);
    }

    private String buildSitemap() {
        List<Entry> entries = new ArrayList<>();
        for (String path : List.of("/", "/tutorials", "/blog", "/portfolio", "/english", "/english/grammar", "/english/reading", "/english/listening", "/english/listening/pronunciation", "/english/writing", "/english/bundles", "/about")) entries.add(new Entry(path, null));
        repository.publishedEntries().forEach(entry -> entries.add(new Entry(entry.path(), entry.updatedAt())));

        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        entries.stream().distinct().forEach(entry -> {
            xml.append("  <url><loc>").append(escape(properties.siteOrigin() + entry.path())).append("</loc>");
            if (entry.updatedAt() != null) xml.append("<lastmod>").append(entry.updatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append("</lastmod>");
            xml.append("</url>\n");
        });
        return xml.append("</urlset>\n").toString();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void contentChanged(SeoContentChangedEvent ignored) {
        cache.clear();
    }

    private String escape(String value) { return HtmlUtils.htmlEscape(value); }
    private record Entry(String path, LocalDateTime updatedAt) {}
}
