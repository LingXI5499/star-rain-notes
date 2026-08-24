package com.starrainnotes.english.reading.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.reading.dto.ReadingAdminStats;
import com.starrainnotes.english.reading.dto.ReadingArticleLinkView;
import com.starrainnotes.english.reading.dto.ReadingArticleRequest;
import com.starrainnotes.english.reading.dto.ReadingArticleSummaryView;
import com.starrainnotes.english.reading.dto.ReadingArticleView;
import com.starrainnotes.english.reading.dto.ReadingGrammarRef;
import com.starrainnotes.english.reading.dto.ReadingHomeView;
import com.starrainnotes.english.reading.dto.ReadingPageView;
import com.starrainnotes.english.reading.dto.ReadingTagRef;
import com.starrainnotes.english.reading.entity.ReadingArticle;
import com.starrainnotes.english.reading.mapper.ReadingArticleMapper;
import com.starrainnotes.english.reading.support.ReadingTextStatistics;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reading article domain (方案 §6, §9.3, §11).
 *
 * <p>Stats are recomputed server-side from the Markdown body on every save.
 * Create/update accept tag ids which are validated against the shared taxonomy
 * dimensions; publish-time checks (title/slug/summary/body/level/CEFR/Topic/
 * Genre/cover/source) run in the service layer, never only in the UI. Published
 * articles cannot be physically deleted until withdrawn. A published article is
 * the only thing reachable through public endpoints.</p>
 */
@Service
public class ReadingArticleService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String WITHDRAWN = "WITHDRAWN";
    private static final int MAX_PAGE_SIZE = 50;

    private static final String ARTICLE_SELECT = """
            SELECT a.id,a.title,a.slug,a.summary,a.body_markdown,a.cover_media_id,
                   m.public_url AS cover_url,a.reading_level,a.cefr_level,a.source_name,
                   a.source_url,a.copyright_note,a.word_count,a.unique_word_count,
                   a.average_sentence_words,a.max_sentence_words,a.estimated_minutes,
                   a.publish_status,a.sort_order,a.published_at,a.updated_at
            FROM english_reading_article a
            LEFT JOIN media_asset m ON m.id=a.cover_media_id
            """;

    private final ReadingArticleMapper mapper;
    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;

    public ReadingArticleService(ReadingArticleMapper mapper, JdbcTemplate jdbc, SiteSettingsTimezone timezone) {
        this.mapper = mapper;
        this.jdbc = jdbc;
        this.timezone = timezone;
    }

    @Transactional
    public ReadingArticleView create(ReadingArticleRequest request) {
        ReadingTextStatistics.Stats stats = ReadingTextStatistics.analyze(request.bodyMarkdown());
        assertSlugFree(request.slug().trim(), null);
        validateCefr(request.cefrLevel());
        validateReadingLevel(request.readingLevel());
        validateCover(request.coverMediaId());
        ReadingArticle article = new ReadingArticle();
        applyFields(article, request, stats);
        article.setPublishStatus(DRAFT);
        if (article.getSortOrder() == null) {
            article.setSortOrder(nextSortOrder());
        }
        try {
            mapper.insert(article);
        } catch (DuplicateKeyException ex) {
            throw slugConflict();
        }
        replaceTagsAndGrammar(article.getId(), request);
        return get(article.getId());
    }

    @Transactional
    public ReadingArticleView update(Long id, ReadingArticleRequest request) {
        require(id);
        ReadingTextStatistics.Stats stats = ReadingTextStatistics.analyze(request.bodyMarkdown());
        assertSlugFree(request.slug().trim(), id);
        validateCefr(request.cefrLevel());
        validateReadingLevel(request.readingLevel());
        validateCover(request.coverMediaId());
        ReadingArticle article = mapper.selectById(id);
        applyFields(article, request, stats);
        try {
            mapper.updateById(article);
        } catch (DuplicateKeyException ex) {
            throw slugConflict();
        }
        replaceTagsAndGrammar(id, request);
        return get(id);
    }

    public ReadingArticleView get(Long id) {
        try {
            return jdbc.queryForObject(ARTICLE_SELECT + " WHERE a.id=?", this::mapView, id);
        } catch (EmptyResultDataAccessException ex) {
            throw articleNotFound();
        }
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
        List<ReadingArticleSummaryView> rows = jdbc.query(sql, listParams.toArray(), (rs, row) -> mapSummary(rs));

        Map<Long, List<ReadingTagRef>> tags = loadTags(rows.stream().map(ReadingArticleSummaryView::id).toList());
        Set<Long> withExercises = loadArticleIdsWithExercises(rows.stream().map(ReadingArticleSummaryView::id).toList());

        List<ReadingArticleSummaryView> items = rows.stream()
                .map(row -> row.withTags(tags.getOrDefault(row.id(), List.of()))
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
        return new ReadingHomeView(nvl(total), byLevel, byCefr, filterTags("TOPIC"), filterTags("GENRE"));
    }

    private List<ReadingTagRef> filterTags(String dimension) {
        return jdbc.query("""
                SELECT id,name,slug,dimension,'TAG' AS tag_role FROM english_taxonomy_term
                WHERE dimension=? AND enabled=1 AND parent_id IS NULL ORDER BY sort_order,id
                """, (rs, row) -> new ReadingTagRef(rs.getLong("id"), rs.getString("name"),
                rs.getString("slug"), rs.getString("dimension"), rs.getString("tag_role")), dimension);
    }

    public ReadingPageView publicList(int page, int pageSize, String q, Integer level, String cefr,
                                      Long topic, Long genre) {        List<Object> params = new ArrayList<>();
        String where = buildListWhere(q, null, level, cefr, topic, genre, params) + " AND a.publish_status='PUBLISHED'";
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM english_reading_article a " + where,
                Long.class, params.toArray());
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        String sql = ARTICLE_SELECT + where + " ORDER BY a.sort_order, a.id LIMIT " + safeSize + " OFFSET "
                + ((safePage - 1) * safeSize);
        List<ReadingArticleSummaryView> rows = jdbc.query(sql, params.toArray(), (rs, row) -> mapSummary(rs));
        Map<Long, List<ReadingTagRef>> tags = loadTags(rows.stream().map(ReadingArticleSummaryView::id).toList());
        List<ReadingArticleSummaryView> items = rows.stream()
                .map(row -> row.withTags(tags.getOrDefault(row.id(), List.of())))
                .toList();
        int totalPages = total == 0 ? 0 : (int) ((total + safeSize - 1) / safeSize);
        return new ReadingPageView(items, safePage, safeSize, total, totalPages, null);
    }

    @Transactional
    public ReadingArticleView publish(Long id) {
        ReadingArticle article = require(id);
        List<String> problems = publishProblems(article);
        if (!problems.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_READING_PUBLISH_INVALID",
                    "Cannot publish", String.join("; ", problems));
        }
        jdbc.update("""
                UPDATE english_reading_article
                SET publish_status='PUBLISHED', published_at=COALESCE(published_at, UTC_TIMESTAMP(6))
                WHERE id=?
                """, id);
        return get(id);
    }

    @Transactional
    public ReadingArticleView withdraw(Long id) {
        ReadingArticle article = require(id);
        if (DRAFT.equals(article.getPublishStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_INVALID_PUBLISH_TRANSITION",
                    "Invalid publish transition", "A draft article cannot be withdrawn.");
        }
        jdbc.update("UPDATE english_reading_article SET publish_status='WITHDRAWN' WHERE id=?", id);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        ReadingArticle article = require(id);
        if (PUBLISHED.equals(article.getPublishStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_READING_PUBLISHED_DELETE_FORBIDDEN",
                    "Published article cannot be deleted",
                    "Withdraw the article before deleting it.");
        }
        mapper.deleteById(id);
    }

    // ---------------------------------------------------------------
    // publish checks (service layer, §11.3)
    // ---------------------------------------------------------------

    private List<String> publishProblems(ReadingArticle article) {
        List<String> problems = new ArrayList<>();
        if (isBlank(article.getTitle())) problems.add("标题不能为空");
        if (isBlank(article.getSlug())) problems.add("slug 不能为空");
        if (isBlank(article.getSummary())) problems.add("摘要不能为空");
        if (isBlank(article.getBodyMarkdown())) problems.add("正文不能为空");
        if (article.getReadingLevel() == null || article.getReadingLevel() < 1 || article.getReadingLevel() > 3) {
            problems.add("能力层级必须为1/2/3");
        }
        if (isBlank(article.getCefrLevel())) problems.add("CEFR 等级不能为空");
        if (!hasDimensionTag(idOrZero(article), "TOPIC")) problems.add("至少需要一个主题(TOPIC)标签");
        if (!hasDimensionTag(idOrZero(article), "GENRE")) problems.add("至少需要一个文体(GENRE)标签");
        if (article.getCoverMediaId() != null && !isImage(article.getCoverMediaId())) {
            problems.add("封面必须为图片");
        }
        return problems;
    }

    private Long idOrZero(ReadingArticle article) {
        return article.getId() == null ? 0L : article.getId();
    }

    private boolean hasDimensionTag(Long articleId, String dimension) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM english_reading_article_tag at
                JOIN english_taxonomy_term t ON t.id=at.term_id
                WHERE at.article_id=? AND t.dimension=? AND t.enabled=1
                """, Integer.class, articleId, dimension);
        return count != null && count > 0;
    }

    // ---------------------------------------------------------------
    // tag / validation helpers
    // ---------------------------------------------------------------

    private void replaceTagsAndGrammar(Long articleId, ReadingArticleRequest request) {
        jdbc.update("DELETE FROM english_reading_article_tag WHERE article_id=?", articleId);
        jdbc.update("DELETE FROM english_reading_article_grammar_lesson WHERE article_id=?", articleId);

        boolean primaryAssigned = false;
        for (Long termId : nonNull(request.topicTagIds())) {
            requireEnabledTag(termId, "TOPIC");
            jdbc.update("INSERT INTO english_reading_article_tag(article_id,term_id,tag_role) VALUES (?,?,?)",
                    articleId, termId, primaryAssigned ? "TAG" : "PRIMARY");
            primaryAssigned = true;
        }
        for (Long termId : nonNull(request.genreTagIds())) {
            requireEnabledTag(termId, "GENRE");
            jdbc.update("INSERT INTO english_reading_article_tag(article_id,term_id,tag_role) VALUES (?,?,?)",
                    articleId, termId, "TAG");
        }
        for (Long termId : nonNull(request.abilityTagIds())) {
            requireEnabledTag(termId, "ABILITY");
            jdbc.update("INSERT INTO english_reading_article_tag(article_id,term_id,tag_role) VALUES (?,?,?)",
                    articleId, termId, "TAG");
        }
        for (Long lessonId : nonNull(request.grammarLessonIds())) {
            requireGrammarLesson(lessonId);
            jdbc.update("INSERT INTO english_reading_article_grammar_lesson(article_id,lesson_id) VALUES (?,?)",
                    articleId, lessonId);
        }
    }

    private void requireEnabledTag(Long termId, String expectedDimension) {
        List<String> found = jdbc.query("""
                SELECT dimension FROM english_taxonomy_term WHERE id=? AND enabled=1
                """, (rs, row) -> rs.getString("dimension"), termId);
        if (found.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_NOT_FOUND",
                    "Invalid taxonomy term", "The selected tag does not exist or is disabled.");
        }
        if (!expectedDimension.equals(found.get(0))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_DEPTH_INVALID",
                    "Invalid tag dimension",
                    "A " + expectedDimension + " tag cannot reference the '" + found.get(0) + "' dimension.");
        }
    }

    private List<Long> nonNull(List<Long> ids) {
        return ids == null ? List.of() : ids;
    }

    private void requireGrammarLesson(Long lessonId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_grammar_lesson WHERE id=?", Integer.class, lessonId);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_READING_GRAMMAR_LESSON_INVALID",
                    "Invalid grammar lesson", "The selected grammar lesson does not exist.");
        }
    }

    private void validateCefr(String cefr) {
        if (cefr == null) return;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_cefr_standard WHERE level=?", Integer.class, cefr);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_CEFR_INVALID",
                    "Invalid CEFR level", "The selected CEFR level does not exist.");
        }
    }

    private void validateReadingLevel(Integer level) {
        if (level == null || level < 1 || level > 3) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_READING_LEVEL_INVALID",
                    "Invalid reading level", "Reading level must be 1, 2 or 3.");
        }
    }

    private void validateCover(Long mediaId) {
        if (mediaId == null) return;
        if (!isImage(mediaId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_COVER_MEDIA",
                    "Invalid cover", "The selected cover must be an existing image asset.");
        }
    }

    private boolean isImage(Long mediaId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM media_asset WHERE id=? AND asset_type='IMAGE'", Integer.class, mediaId);
        return count != null && count > 0;
    }

    private void assertSlugFree(String slug, Long excludedId) {
        LambdaQueryWrapper<ReadingArticle> wrapper = new LambdaQueryWrapper<ReadingArticle>()
                .eq(ReadingArticle::getSlug, slug);
        if (excludedId != null) wrapper.ne(ReadingArticle::getId, excludedId);
        Long count = mapper.selectCount(wrapper);
        if (count != null && count > 0) throw slugConflict();
    }

    private void applyFields(ReadingArticle article, ReadingArticleRequest request,
                             ReadingTextStatistics.Stats stats) {
        article.setTitle(request.title().trim());
        article.setSlug(request.slug().trim());
        article.setSummary(request.summary().trim());
        article.setBodyMarkdown(request.bodyMarkdown());
        article.setCoverMediaId(request.coverMediaId());
        article.setReadingLevel(request.readingLevel());
        article.setCefrLevel(request.cefrLevel());
        article.setSourceName(clean(request.sourceName()));
        article.setSourceUrl(clean(request.sourceUrl()));
        article.setCopyrightNote(clean(request.copyrightNote()));
        article.setWordCount(stats.wordCount());
        article.setUniqueWordCount(stats.uniqueWordCount());
        article.setAverageSentenceWords(BigDecimal.valueOf(stats.averageSentenceWords()));
        article.setMaxSentenceWords(stats.maxSentenceWords());
        article.setEstimatedMinutes(stats.estimatedMinutes());
        if (request.sortOrder() != null) article.setSortOrder(request.sortOrder());
    }

    private Integer nextSortOrder() {
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_order),0) FROM english_reading_article", Integer.class);
        return (max == null ? 0 : max) + 10;
    }

    // ---------------------------------------------------------------
    // list filters + aggregates
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // batch tag loading (avoid N+1)
    // ---------------------------------------------------------------

    private Map<Long, List<ReadingTagRef>> loadTags(List<Long> articleIds) {
        if (articleIds.isEmpty()) return Map.of();
        String ids = String.join(",", articleIds.stream().map(String::valueOf).toList());
        Map<Long, List<ReadingTagRef>> result = new LinkedHashMap<>();
        jdbc.query("""
                SELECT at.article_id, t.id, t.name, t.slug, t.dimension, at.tag_role
                FROM english_reading_article_tag at
                JOIN english_taxonomy_term t ON t.id=at.term_id
                WHERE at.article_id IN (""" + ids + ") ORDER BY t.dimension, t.sort_order, t.id",
                rs -> {
                    while (rs.next()) {
                        result.computeIfAbsent(rs.getLong("article_id"), k -> new ArrayList<>())
                                .add(new ReadingTagRef(rs.getLong("id"), rs.getString("name"), rs.getString("slug"),
                                        rs.getString("dimension"), rs.getString("tag_role")));
                    }
                    return null;
                });
        return result;
    }

    private Set<Long> loadArticleIdsWithExercises(List<Long> articleIds) {
        if (articleIds.isEmpty()) return Set.of();
        String ids = String.join(",", articleIds.stream().map(String::valueOf).toList());
        Set<Long> result = new LinkedHashSet<>();
        jdbc.query("SELECT DISTINCT article_id FROM english_reading_article_exercise WHERE article_id IN (" + ids + ")",
                rs -> {
                    while (rs.next()) {
                        result.add(rs.getLong("article_id"));
                    }
                    return null;
                });
        return result;
    }

    // ---------------------------------------------------------------
    // mapping
    // ---------------------------------------------------------------

    private ReadingArticleSummaryView mapSummary(ResultSet rs) throws SQLException {
        return new ReadingArticleSummaryView(rs.getLong("id"), rs.getString("title"), rs.getString("slug"),
                rs.getString("summary"), rs.getString("cover_url"), rs.getInt("reading_level"),
                rs.getString("cefr_level"), rs.getInt("word_count"), rs.getInt("estimated_minutes"),
                rs.getString("publish_status"), false, format(rs.getTimestamp("updated_at")), List.of());
    }

    private ReadingArticleView mapView(ResultSet rs, int row) throws SQLException {
        Long id = rs.getLong("id");
        return new ReadingArticleView(id, rs.getString("title"), rs.getString("slug"), rs.getString("summary"),
                rs.getString("body_markdown"), nullableLong(rs, "cover_media_id"), rs.getString("cover_url"),
                rs.getInt("reading_level"), rs.getString("cefr_level"), rs.getString("source_name"),
                rs.getString("source_url"), rs.getString("copyright_note"), rs.getInt("word_count"),
                rs.getInt("unique_word_count"), rs.getBigDecimal("average_sentence_words"),
                rs.getInt("max_sentence_words"), rs.getInt("estimated_minutes"), rs.getString("publish_status"),
                rs.getInt("sort_order"), format(rs.getTimestamp("published_at")),
                format(rs.getTimestamp("updated_at")), loadTags(List.of(id)).getOrDefault(id, List.of()),
                loadGrammarRefs(id), null, null);
    }

    private List<ReadingGrammarRef> loadGrammarRefs(Long articleId) {
        return jdbc.query("""
                SELECT gl.id, gl.title, gl.slug FROM english_reading_article_grammar_lesson agl
                JOIN english_grammar_lesson gl ON gl.id=agl.lesson_id
                WHERE agl.article_id=? ORDER BY gl.sort_order, gl.id
                """, (rs, row) -> new ReadingGrammarRef(rs.getLong("id"), rs.getString("title"),
                rs.getString("slug")), articleId);
    }

    private ReadingArticle require(Long id) {
        ReadingArticle article = mapper.selectById(id);
        if (article == null) throw articleNotFound();
        return article;
    }

    private ApiException articleNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_READING_ARTICLE_NOT_FOUND",
                "Article not found", "The reading article does not exist.");
    }

    private ApiException slugConflict() {
        return new ApiException(HttpStatus.CONFLICT, "ENGLISH_CONTENT_SLUG_CONFLICT",
                "Slug already in use", "Choose another stable slug.");
    }

    private String format(java.sql.Timestamp ts) {
        return ts == null ? null : timezone.atSite(ts.toLocalDateTime()).format(ISO_OFFSET);
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
