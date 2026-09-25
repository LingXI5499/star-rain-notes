package com.starrainnotes.seo;

import com.starrainnotes.blog.api.BlogSeoPort;
import com.starrainnotes.english.grammar.api.GrammarSeoPort;
import com.starrainnotes.english.listening.api.ListeningSeoPort;
import com.starrainnotes.english.reading.api.ReadingSeoPort;
import com.starrainnotes.english.shared.bundle.api.BundleSeoPort;
import com.starrainnotes.english.writing.api.WritingSeoPort;
import com.starrainnotes.portfolio.api.PortfolioSeoPort;
import com.starrainnotes.tutorial.api.TutorialSeoPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final SeoProperties properties;
    private final TutorialSeoPort tutorials;
    private final BlogSeoPort blogs;
    private final PortfolioSeoPort projects;
    private final GrammarSeoPort grammar;
    private final ReadingSeoPort reading;
    private final ListeningSeoPort listening;
    private final WritingSeoPort writing;
    private final BundleSeoPort bundles;
    private final BoundedSeoCache<String> cache = new BoundedSeoCache<>(1, CACHE_MILLIS, System::currentTimeMillis);

    public SeoSitemapService(SeoProperties properties, TutorialSeoPort tutorials, BlogSeoPort blogs, PortfolioSeoPort projects,
                             GrammarSeoPort grammar, ReadingSeoPort reading, ListeningSeoPort listening,
                             WritingSeoPort writing, BundleSeoPort bundles) {
        this.properties = properties;
        this.tutorials = tutorials;
        this.blogs = blogs;
        this.projects = projects;
        this.grammar = grammar;
        this.reading = reading;
        this.listening = listening;
        this.writing = writing;
        this.bundles = bundles;
    }

    @Transactional(readOnly = true)
    public String sitemap() {
        return cache.get("sitemap", this::buildSitemap);
    }

    private String buildSitemap() {
        List<Entry> entries = new ArrayList<>();
        for (String path : List.of("/", "/tutorials", "/blog", "/portfolio", "/english", "/english/grammar", "/english/reading", "/english/listening", "/english/listening/pronunciation", "/english/writing", "/english/bundles", "/about")) entries.add(new Entry(path, null));
        tutorials.publishedTutorials().forEach(row -> entries.add(new Entry("/tutorials/" + row.slug(), row.updatedAt())));
        tutorials.publishedChapterPaths().forEach(row -> entries.add(new Entry("/tutorials/" + row.tutorialSlug() + "/" + row.chapterSlug(), row.updatedAt())));
        blogs.published().forEach(row -> entries.add(new Entry("/blog/" + row.slug(), row.updatedAt())));
        projects.published().forEach(row -> entries.add(new Entry("/portfolio/" + row.slug(), row.updatedAt())));
        grammar.published().forEach(row -> entries.add(new Entry("/english/grammar/" + row.slug(), row.updatedAt())));
        reading.published().forEach(row -> entries.add(new Entry("/english/reading/" + row.slug(), row.updatedAt())));
        listening.publishedMaterials().forEach(row -> entries.add(new Entry("/english/listening/" + row.slug(), row.updatedAt())));
        listening.publishedRules().forEach(row -> entries.add(new Entry("/english/listening/pronunciation/" + row.slug(), row.updatedAt())));
        writing.publishedResources().forEach(row -> entries.add(new Entry("/english/writing/resources/" + row.slug(), row.updatedAt())));
        writing.publishedPrompts().forEach(row -> entries.add(new Entry("/english/writing/practice/" + row.slug(), row.updatedAt())));
        bundles.published().forEach(row -> entries.add(new Entry("/english/bundles/" + row.slug(), row.updatedAt())));

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
