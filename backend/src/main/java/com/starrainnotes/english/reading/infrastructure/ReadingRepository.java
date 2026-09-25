package com.starrainnotes.english.reading.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.api.MediaPort;
import com.starrainnotes.english.reading.dto.ReadingAdminStats;
import com.starrainnotes.english.reading.dto.ReadingArticleLinkView;
import com.starrainnotes.english.reading.dto.ReadingArticleSummaryView;
import com.starrainnotes.english.reading.dto.ReadingArticleView;
import com.starrainnotes.english.reading.dto.ReadingHomeView;
import com.starrainnotes.english.reading.dto.ReadingPageView;
import com.starrainnotes.english.reading.dto.ReadingTagRef;
import com.starrainnotes.english.reading.entity.ReadingArticle;
import com.starrainnotes.english.reading.domain.ReadingContentPort;
import com.starrainnotes.english.shared.content.ContentDescriptor;
import com.starrainnotes.english.shared.content.ContentCatalogFilter;
import com.starrainnotes.english.shared.content.ContentCatalogSlice;
import com.starrainnotes.english.shared.content.EnglishContentType;
import com.starrainnotes.english.reading.mapper.ReadingArticleMapper;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** SQL and MyBatis persistence for reading articles and read projections. */
@Repository
public class ReadingRepository implements ReadingContentPort {
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int MAX_PAGE_SIZE = 50;

    private static final String ARTICLE_SELECT = """
            SELECT a.id,a.title,a.slug,a.summary,a.body_markdown,a.cover_media_id,
                   a.reading_level,a.cefr_level,a.source_name,
                   a.source_url,a.copyright_note,a.word_count,a.unique_word_count,
                   a.average_sentence_words,a.max_sentence_words,a.estimated_minutes,
                   a.publish_status,a.sort_order,a.published_at,a.updated_at
            FROM english_reading_article a
            """;

    private final ReadingArticleMapper mapper;
    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;
    private final MediaPort media;
    private final ReadingRelationRepository relations;

    public ReadingRepository(ReadingArticleMapper mapper, JdbcTemplate jdbc,
                             SiteSettingsTimezone timezone, MediaPort media,
                             ReadingRelationRepository relations) {
        this.mapper = mapper;
        this.jdbc = jdbc;
        this.timezone = timezone;
        this.media = media;
        this.relations = relations;
    }

    public ReadingArticleView get(Long id) {
        try {
            return jdbc.queryForObject(ARTICLE_SELECT + " WHERE a.id=?", this::mapView, id);
        } catch (EmptyResultDataAccessException ex) {
            throw articleNotFound();
        }
    }

    public ContentCatalogSlice catalogDescriptors(ContentCatalogFilter filter, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (filter.status() != null) {
            where.append(" AND a.publish_status=?");
            args.add(filter.status());
        }
        if (filter.cefr() != null) {
            where.append(" AND a.cefr_level=?");
            args.add(filter.cefr());
        }
        if (filter.term() != null) {
            where.append(" AND (LOWER(a.title) LIKE ? OR LOWER(a.slug) LIKE ? OR LOWER(a.summary) LIKE ?)");
            String term = "%" + filter.term() + "%";
            args.add(term);
            args.add(term);
            args.add(term);
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM english_reading_article a" + where,
                Long.class, args.toArray());
        if (limit <= 0) return new ContentCatalogSlice(total == null ? 0 : total, List.of());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(limit);
        Map<Long, Long> coverIds = new HashMap<>();
        List<ContentDescriptor> items = jdbc.query("""
                SELECT a.id,a.slug,a.title,a.summary,a.cefr_level,a.cover_media_id,
                       a.publish_status,a.sort_order
                FROM english_reading_article a
                """ + where + " ORDER BY FIELD(a.publish_status,'PUBLISHED','DRAFT','WITHDRAWN'),a.sort_order,a.id LIMIT ?",
                (rs, row) -> {
                    long id = rs.getLong("id");
                    long coverId = rs.getLong("cover_media_id");
                    if (!rs.wasNull()) coverIds.put(id, coverId);
                    return new ContentDescriptor(EnglishContentType.READING,
                            id, rs.getString("slug"), rs.getString("title"), rs.getString("summary"),
                            rs.getString("cefr_level"), null, rs.getString("publish_status"),
                            rs.getInt("sort_order"));
                }, pageArgs.toArray());
        Map<Long, String> coverUrls = media.publicUrls(coverIds.values());
        return new ContentCatalogSlice(total == null ? 0 : total, items.stream()
                .map(item -> item.withCoverUrl(coverIds.containsKey(item.id())
                        ? coverUrls.get(coverIds.get(item.id())) : null)).toList());
    }

    public ReadingArticleView publicGet(String slug) {
        try {
            ReadingArticleView current = jdbc.queryForObject(
                    ARTICLE_SELECT + " WHERE a.slug=? AND a.publish_status='PUBLISHED'", this::mapView, slug);
            return attachNavigation(current);
        } catch (EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_CONTENT_NOT_PUBLISHED",
                    "Article not available", "The requested article is not published.");
        }
    }

    public ReadingPageView list(int page, int pageSize, String q, String status,
                                Integer level, String cefr, Long topic, Long genre) {
        List<Object> params = new ArrayList<>();
        String where = buildListWhere(q, status, level, cefr, topic, genre, params);

        long total = jdbc.queryForObject("SELECT COUNT(*) FROM english_reading_article a " + where,
                Long.class, params.toArray());

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;
        String sql = ARTICLE_SELECT + where + " ORDER BY a.sort_order, a.id LIMIT " + safeSize + " OFFSET " + offset;
        List<Object> listParams = new ArrayList<>(params);
        Map<Long, Long> coverIds = new HashMap<>();
        List<ReadingArticleSummaryView> rows = jdbc.query(sql, listParams.toArray(), (rs, row) -> {
            coverIds.put(rs.getLong("id"), nullableLong(rs, "cover_media_id"));
            return mapSummary(rs);
        });
        Map<Long, String> coverUrls = media.publicUrls(coverIds.values());

        Map<Long, List<ReadingTagRef>> tags = relations.findTagsByArticleIds(
                rows.stream().map(ReadingArticleSummaryView::id).toList());
        Set<Long> withExercises = loadArticleIdsWithExercises(rows.stream().map(ReadingArticleSummaryView::id).toList());

        List<ReadingArticleSummaryView> items = rows.stream()
                .map(row -> row.withCoverUrl(coverUrl(row.id(), coverIds, coverUrls))
                        .withTags(tags.getOrDefault(row.id(), List.of()))
                        .withExercises(withExercises.contains(row.id())))
                .toList();
        int totalPages = total == 0 ? 0 : (int) ((total + safeSize - 1) / safeSize);
        return new ReadingPageView(items, safePage, safeSize, total, totalPages, adminStats());
    }

    public ReadingHomeView home() {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_reading_article WHERE publish_status='PUBLISHED'", Long.class);
        Map<Integer, Long> byLevel = new LinkedHashMap<>();
        jdbc.query("SELECT reading_level, COUNT(*) FROM english_reading_article"
                        + " WHERE publish_status='PUBLISHED' GROUP BY reading_level ORDER BY reading_level",
                rs -> { while (rs.next()) byLevel.put(rs.getInt(1), rs.getLong(2)); return null; });
        Map<String, Long> byCefr = new LinkedHashMap<>();
        jdbc.query("SELECT cefr_level, COUNT(*) FROM english_reading_article"
                        + " WHERE publish_status='PUBLISHED' GROUP BY cefr_level ORDER BY cefr_level",
                rs -> { while (rs.next()) byCefr.put(rs.getString(1), rs.getLong(2)); return null; });
        return new ReadingHomeView(nvl(total), byLevel, byCefr,
                relations.findRootTagsByDimension("TOPIC"), relations.findRootTagsByDimension("GENRE"));
    }

    public ReadingPageView publicList(int page, int pageSize, String q, Integer level, String cefr,
                                      Long topic, Long genre) {
        List<Object> params = new ArrayList<>();
        String where = buildListWhere(q, null, level, cefr, topic, genre, params) + " AND a.publish_status='PUBLISHED'";
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM english_reading_article a " + where,
                Long.class, params.toArray());
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        String sql = ARTICLE_SELECT + where + " ORDER BY a.sort_order, a.id LIMIT " + safeSize + " OFFSET "
                + ((safePage - 1) * safeSize);
        Map<Long, Long> coverIds = new HashMap<>();
        List<ReadingArticleSummaryView> rows = jdbc.query(sql, params.toArray(), (rs, row) -> {
            coverIds.put(rs.getLong("id"), nullableLong(rs, "cover_media_id"));
            return mapSummary(rs);
        });
        Map<Long, String> coverUrls = media.publicUrls(coverIds.values());
        Map<Long, List<ReadingTagRef>> tags = relations.findTagsByArticleIds(
                rows.stream().map(ReadingArticleSummaryView::id).toList());
        List<ReadingArticleSummaryView> items = rows.stream()
                .map(row -> row.withCoverUrl(coverUrl(row.id(), coverIds, coverUrls))
                        .withTags(tags.getOrDefault(row.id(), List.of())))
                .toList();
        int totalPages = total == 0 ? 0 : (int) ((total + safeSize - 1) / safeSize);
        return new ReadingPageView(items, safePage, safeSize, total, totalPages, null);
    }

    public Integer nextSortOrder() {
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_order),0) FROM english_reading_article", Integer.class);
        return (max == null ? 0 : max) + 10;
    }

    public boolean slugExists(String slug, Long excludedId) {
        LambdaQueryWrapper<ReadingArticle> wrapper =
                new LambdaQueryWrapper<ReadingArticle>().eq(ReadingArticle::getSlug, slug);
        if (excludedId != null) wrapper.ne(ReadingArticle::getId, excludedId);
        Long count = mapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private String buildListWhere(String q, String status, Integer level, String cefr,
                                  Long topic, Long genre, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (q != null && !q.isBlank()) {
            String p = "%" + q.trim() + "%";
            where.append(" AND (a.title LIKE ? OR a.summary LIKE ?)");
            params.add(p);
            params.add(p);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND a.publish_status=?");
            params.add(status);
        }
        if (level != null) {
            where.append(" AND a.reading_level=?");
            params.add(level);
        }
        if (cefr != null && !cefr.isBlank()) {
            where.append(" AND a.cefr_level=?");
            params.add(cefr);
        }
        if (topic != null) {
            where.append(" AND EXISTS (SELECT 1 FROM english_reading_article_tag rt JOIN english_taxonomy_term tt"
                    + " ON tt.id=rt.term_id WHERE rt.article_id=a.id AND rt.term_id=? AND tt.dimension='TOPIC')");
            params.add(topic);
        }
        if (genre != null) {
            where.append(" AND EXISTS (SELECT 1 FROM english_reading_article_tag rt JOIN english_taxonomy_term tt"
                    + " ON tt.id=rt.term_id WHERE rt.article_id=a.id AND rt.term_id=? AND tt.dimension='GENRE')");
            params.add(genre);
        }
        return where.toString();
    }

    private ReadingAdminStats adminStats() {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM english_reading_article", Long.class);
        Long published = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_reading_article WHERE publish_status='PUBLISHED'", Long.class);
        Long draft = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_reading_article WHERE publish_status='DRAFT'", Long.class);
        Long withdrawn = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_reading_article WHERE publish_status='WITHDRAWN'", Long.class);
        Long missing = jdbc.queryForObject("""
                SELECT COUNT(*) FROM english_reading_article a
                WHERE NOT EXISTS (SELECT 1 FROM english_reading_article_exercise e WHERE e.article_id=a.id)
                """, Long.class);
        Map<String, Long> byCefr = new LinkedHashMap<>();
        jdbc.query("SELECT cefr_level, COUNT(*) FROM english_reading_article GROUP BY cefr_level ORDER BY cefr_level",
                rs -> {
                    while (rs.next()) {
                        byCefr.put(rs.getString(1), rs.getLong(2));
                    }
                    return null;
                });
        return new ReadingAdminStats(nvl(total), nvl(published), nvl(draft), nvl(withdrawn),
                nvl(missing), byCefr);
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    // ---------------------------------------------------------------
    // navigation (prev / next among published, public detail only)
    // ---------------------------------------------------------------

    private ReadingArticleView attachNavigation(ReadingArticleView current) {
        List<Long> publishedIds = jdbc.queryForList("""
                SELECT id FROM english_reading_article
                WHERE publish_status='PUBLISHED' ORDER BY sort_order, id
                """, Long.class);
        int index = publishedIds.indexOf(current.id());
        ReadingArticleLinkView previous = index > 0 ? link(publishedIds.get(index - 1)) : null;
        ReadingArticleLinkView next = index >= 0 && index + 1 < publishedIds.size()
                ? link(publishedIds.get(index + 1)) : null;
        return current.withNavigation(previous, next);
    }

    private ReadingArticleLinkView link(Long id) {
        return jdbc.queryForObject(
                "SELECT title, slug FROM english_reading_article WHERE id=?",
                (rs, row) -> new ReadingArticleLinkView(rs.getString("title"), rs.getString("slug")), id);
    }

    private Set<Long> loadArticleIdsWithExercises(List<Long> articleIds) {
        if (articleIds.isEmpty()) return Set.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(articleIds.size(), "?"));
        Set<Long> result = new LinkedHashSet<>();
        jdbc.query("SELECT DISTINCT article_id FROM english_reading_article_exercise WHERE article_id IN ("
                        + placeholders + ")",
                rs -> {
                    while (rs.next()) {
                        result.add(rs.getLong("article_id"));
                    }
                    return null;
                }, articleIds.toArray());
        return result;
    }

    // ---------------------------------------------------------------
    // mapping
    // ---------------------------------------------------------------

    private ReadingArticleSummaryView mapSummary(ResultSet rs) throws SQLException {
        return new ReadingArticleSummaryView(rs.getLong("id"), rs.getString("title"), rs.getString("slug"),
                rs.getString("summary"), null, rs.getInt("reading_level"),
                rs.getString("cefr_level"), rs.getInt("word_count"), rs.getInt("estimated_minutes"),
                rs.getString("publish_status"), false, format(rs.getTimestamp("updated_at")), List.of());
    }

    private String coverUrl(Long articleId, Map<Long, Long> coverIds, Map<Long, String> coverUrls) {
        Long mediaId = coverIds.get(articleId);
        return mediaId == null ? null : coverUrls.get(mediaId);
    }

    private ReadingArticleView mapView(ResultSet rs, int row) throws SQLException {
        Long id = rs.getLong("id");
        return new ReadingArticleView(id, rs.getString("title"), rs.getString("slug"), rs.getString("summary"),
                rs.getString("body_markdown"), nullableLong(rs, "cover_media_id"),
                media.publicUrl(nullableLong(rs, "cover_media_id")),
                rs.getInt("reading_level"), rs.getString("cefr_level"), rs.getString("source_name"),
                rs.getString("source_url"), rs.getString("copyright_note"), rs.getInt("word_count"),
                rs.getInt("unique_word_count"), rs.getBigDecimal("average_sentence_words"),
                rs.getInt("max_sentence_words"), rs.getInt("estimated_minutes"), rs.getString("publish_status"),
                rs.getInt("sort_order"), format(rs.getTimestamp("published_at")),
                format(rs.getTimestamp("updated_at")),
                relations.findTagsByArticleIds(List.of(id)).getOrDefault(id, List.of()),
                relations.findGrammarRefs(id), null, null);
    }

    public ReadingArticle require(Long id) {
        ReadingArticle article = mapper.selectById(id);
        if (article == null) throw articleNotFound();
        return article;
    }

    private ApiException articleNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_READING_ARTICLE_NOT_FOUND",
                "Article not found", "The reading article does not exist.");
    }

    private String format(java.sql.Timestamp ts) {
        return ts == null ? null : timezone.atSite(ts.toLocalDateTime()).format(ISO_OFFSET);
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    public void insert(ReadingArticle article) { mapper.insert(article); }
    public void update(ReadingArticle article) { mapper.updateById(article); }
    public void publish(Long id) {
        jdbc.update("""
                UPDATE english_reading_article
                SET publish_status='PUBLISHED', published_at=COALESCE(published_at, UTC_TIMESTAMP(6))
                WHERE id=?
                """, id);
    }
    public void withdraw(Long id) {
        jdbc.update("UPDATE english_reading_article SET publish_status='WITHDRAWN' WHERE id=?", id);
    }
    public List<Long> boundExerciseIds(Long articleId) {
        return jdbc.queryForList(
                "SELECT exercise_id FROM english_reading_article_exercise WHERE article_id=?", Long.class, articleId);
    }
    public void delete(Long id) {
        mapper.deleteById(id);
    }
    public boolean cefrExists(String cefr) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_cefr_standard WHERE level=?", Integer.class, cefr);
        return count != null && count > 0;
    }
    private String status(Long id) {
        return jdbc.queryForObject("SELECT publish_status FROM english_reading_article WHERE id=?", String.class, id);
    }
    @Override
    public void requireExists(Long articleId) {
        require(articleId);
    }
    @Override
    public void requirePublished(Long articleId) {
        if (!"PUBLISHED".equals(status(articleId))) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_CONTENT_NOT_PUBLISHED",
                    "Article not available", "The article is not published.");
        }
    }
    @Override
    public java.util.Optional<com.starrainnotes.english.reading.domain.ReadingArticleRef> findRef(long articleId) {
        java.util.List<com.starrainnotes.english.reading.domain.ReadingArticleRef> rows = jdbc.query("""
                SELECT id, title, slug, publish_status FROM english_reading_article WHERE id=?
                """, (rs, row) -> new com.starrainnotes.english.reading.domain.ReadingArticleRef(
                rs.getLong("id"), rs.getString("title"), rs.getString("slug"),
                "PUBLISHED".equals(rs.getString("publish_status"))), articleId);
        return rows.stream().findFirst();
    }
}
