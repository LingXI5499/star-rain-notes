package com.starrainnotes.seo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SeoSitemapService {
    private static final long CACHE_MILLIS = 300_000L;
    private final JdbcTemplate jdbc;
    private final SeoProperties properties;
    private volatile CachedSitemap cached;

    public SeoSitemapService(JdbcTemplate jdbc, SeoProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    public String sitemap() {
        long now = System.currentTimeMillis();
        CachedSitemap current = cached;
        if (current != null && current.expiresAt() > now) return current.xml();
        String xml = buildSitemap();
        cached = new CachedSitemap(xml, now + CACHE_MILLIS);
        return xml;
    }

    private String buildSitemap() {
        List<Entry> entries = new ArrayList<>();
        for (String path : List.of("/", "/tutorials", "/blog", "/portfolio", "/english", "/english/grammar", "/english/reading", "/english/listening", "/english/listening/pronunciation", "/english/writing", "/english/bundles", "/about")) entries.add(new Entry(path, null));
        add(entries, "SELECT CONCAT('/tutorials/',slug),updated_at FROM tutorial WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/tutorials/',t.slug,'/',n.slug),n.updated_at FROM tutorial_node n JOIN tutorial t ON t.id=n.tutorial_id WHERE n.node_type='CHAPTER' AND n.publish_status='PUBLISHED' AND t.publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/blog/',slug),updated_at FROM blog_post WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/portfolio/',slug),updated_at FROM portfolio_project WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/grammar/',l.slug),l.updated_at FROM english_grammar_lesson l JOIN english_grammar_course c ON c.id=l.course_id WHERE l.publish_status='PUBLISHED' AND c.publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/reading/',slug),updated_at FROM english_reading_article WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/listening/',slug),updated_at FROM english_listening_item WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/listening/pronunciation/',slug),updated_at FROM english_listening_pronunciation_rule WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/writing/resources/',slug),updated_at FROM english_writing_resource WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/writing/practice/',slug),updated_at FROM english_writing_prompt WHERE publish_status='PUBLISHED'");
        add(entries, "SELECT CONCAT('/english/bundles/',slug),updated_at FROM english_learning_bundle WHERE publish_status='PUBLISHED'");

        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        entries.stream().distinct().forEach(entry -> {
            xml.append("  <url><loc>").append(escape(properties.siteOrigin() + entry.path())).append("</loc>");
            if (entry.updatedAt() != null) xml.append("<lastmod>").append(entry.updatedAt().toLocalDateTime().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append("</lastmod>");
            xml.append("</url>\n");
        });
        return xml.append("</urlset>\n").toString();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void contentChanged(SeoContentChangedEvent ignored) {
        cached = null;
    }

    private void add(List<Entry> entries, String sql) {
        jdbc.query(sql, (org.springframework.jdbc.core.RowCallbackHandler)
                rs -> entries.add(new Entry(rs.getString(1), rs.getTimestamp(2))));
    }
    private String escape(String value) { return HtmlUtils.htmlEscape(value); }
    private record Entry(String path, Timestamp updatedAt) {}
    private record CachedSitemap(String xml, long expiresAt) {}
}
