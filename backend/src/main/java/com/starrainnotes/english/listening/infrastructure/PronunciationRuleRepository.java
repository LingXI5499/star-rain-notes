package com.starrainnotes.english.listening.infrastructure;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.english.listening.dto.PronunciationRuleRequest;
import com.starrainnotes.english.listening.dto.PronunciationRuleView;
import com.starrainnotes.media.api.MediaAssetPort;
import com.starrainnotes.seo.SeoContentChange;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
public class PronunciationRuleRepository {
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";
    private final JdbcTemplate jdbc;
    private final MediaAssetPort mediaAssets;
    private final SiteSettingsTimezone timezone;

    public PronunciationRuleRepository(JdbcTemplate jdbc, MediaAssetPort mediaAssets, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.mediaAssets = mediaAssets;
        this.timezone = timezone;
    }

    private void validateAudio(Long mediaId) {
        if (mediaId != null && !mediaAssets.isType(mediaId, "AUDIO")) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_AUDIO_MEDIA_INVALID",
                    "Invalid audio", "The selected audio must be an AUDIO asset.");
        }
    }

    private Long nullableLong(ResultSet rs, String col) throws SQLException {
        long value = rs.getLong(col);
        return rs.wasNull() ? null : value;
    }

    private String format(Timestamp ts) {
        return ts == null ? null : timezone.atSite(ts.toLocalDateTime()).format(ISO_OFFSET);
    }

    public List<PronunciationRuleView> pronunciationRules(boolean publishedOnly) {
        String status = publishedOnly ? " WHERE publish_status='PUBLISHED'" : "";
        return jdbc.query("""
                SELECT r.id,r.rule_type,r.title,r.slug,r.summary,r.body_markdown,r.audio_media_id,
                       a.public_url AS audio_url,r.publish_status,r.sort_order,r.published_at,r.updated_at
                FROM english_listening_pronunciation_rule r
                LEFT JOIN media_asset a ON a.id=r.audio_media_id
                """ + status + " ORDER BY r.rule_type, r.sort_order, r.id",
                (rs, row) -> mapRule(rs));
    }

    @Transactional
    public PronunciationRuleView createRule(
            PronunciationRuleRequest request) {
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> ruleSlugExists(candidate, null));
        assertRuleSlugFree(slug, null);
        validateRuleType(request.ruleType());
        validateAudio(request.audioMediaId());
        jdbc.update("INSERT INTO english_listening_pronunciation_rule"
                + "(rule_type,title,slug,summary,body_markdown,audio_media_id,publish_status,sort_order)"
                + " VALUES (?,?,?,?,?,?,'DRAFT',?)",
                request.ruleType(), request.title().trim(), slug, request.summary().trim(),
                request.bodyMarkdown(), request.audioMediaId(),
                request.sortOrder() == null ? nextRuleSort() : request.sortOrder());
        return ruleBySlug(slug, false);
    }

    @Transactional
    @SeoContentChange(table = "english_listening_pronunciation_rule", pathPrefix = "/english/listening/pronunciation/")
    public PronunciationRuleView updateRule(Long id,
            PronunciationRuleRequest request) {
        var current = requireRule(id);
        String slug = NumericSlugGenerator.forUpdate(request.slug(), current.slug());
        assertRuleSlugFree(slug, id);
        validateRuleType(request.ruleType());
        validateAudio(request.audioMediaId());
        jdbc.update("UPDATE english_listening_pronunciation_rule SET rule_type=?,title=?,slug=?,summary=?,"
                + " body_markdown=?,audio_media_id=?,sort_order=COALESCE(?,sort_order) WHERE id=?",
                request.ruleType(), request.title().trim(), slug, request.summary().trim(),
                request.bodyMarkdown(), request.audioMediaId(), request.sortOrder(), id);
        return ruleById(id, false);
    }

    @Transactional
    public void deleteRule(Long id) {
        String status = requireRule(id).publishStatus();
        if (PUBLISHED.equals(status)) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_LISTENING_RULE_PUBLISHED_DELETE_FORBIDDEN",
                    "Published rule cannot be deleted", "Withdraw the rule before deleting it.");
        }
        jdbc.update("DELETE FROM english_listening_pronunciation_rule WHERE id=?", id);
    }

    @Transactional
    @SeoContentChange(table = "english_listening_pronunciation_rule", pathPrefix = "/english/listening/pronunciation/")
    public PronunciationRuleView publishRule(Long id) {
        requireRule(id);
        jdbc.update("UPDATE english_listening_pronunciation_rule SET publish_status='PUBLISHED',"
                + " published_at=COALESCE(published_at, UTC_TIMESTAMP(6)) WHERE id=?", id);
        return ruleById(id, false);
    }

    @Transactional
    @SeoContentChange(table = "english_listening_pronunciation_rule", pathPrefix = "/english/listening/pronunciation/")
    public PronunciationRuleView withdrawRule(Long id) {
        String status = requireRule(id).publishStatus();
        if (DRAFT.equals(status)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_INVALID_PUBLISH_TRANSITION",
                    "Invalid publish transition", "A draft rule cannot be withdrawn.");
        }
        jdbc.update("UPDATE english_listening_pronunciation_rule SET publish_status='WITHDRAWN' WHERE id=?", id);
        return ruleById(id, false);
    }

    @Transactional
    public void moveRule(Long id, int targetIndex) {
        requireRule(id);
        List<Long> ids = new ArrayList<>(jdbc.queryForList(
                "SELECT id FROM english_listening_pronunciation_rule ORDER BY rule_type, sort_order, id",
                Long.class));
        ids.remove(id);
        ids.add(Math.min(Math.max(targetIndex, 0), ids.size()), id);
        if (ids.isEmpty()) return;
        jdbc.update("UPDATE english_listening_pronunciation_rule SET sort_order=100000");
        for (int i = 0; i < ids.size(); i++) {
            jdbc.update("UPDATE english_listening_pronunciation_rule SET sort_order=? WHERE id=?",
                    (i + 1) * 10, ids.get(i));
        }
    }

    private PronunciationRuleView requireRule(Long id) {
        try {
            return jdbc.queryForObject("""
                    SELECT r.id,r.rule_type,r.title,r.slug,r.summary,r.body_markdown,r.audio_media_id,
                           a.public_url AS audio_url,r.publish_status,r.sort_order,r.published_at,r.updated_at
                    FROM english_listening_pronunciation_rule r
                    LEFT JOIN media_asset a ON a.id=r.audio_media_id WHERE r.id=?
                    """, (rs, row) -> mapRule(rs), id);
        } catch (EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_LISTENING_RULE_NOT_FOUND",
                    "Rule not found", "The pronunciation rule does not exist.");
        }
    }

    public PronunciationRuleView ruleById(Long id, boolean publishedOnly) {
        return requireRule(id);
    }

    private PronunciationRuleView ruleBySlug(String slug, boolean publishedOnly) {
        String status = publishedOnly ? " AND r.publish_status='PUBLISHED'" : "";
        try {
            return jdbc.queryForObject("""
                    SELECT r.id,r.rule_type,r.title,r.slug,r.summary,r.body_markdown,r.audio_media_id,
                           a.public_url AS audio_url,r.publish_status,r.sort_order,r.published_at,r.updated_at
                    FROM english_listening_pronunciation_rule r
                    LEFT JOIN media_asset a ON a.id=r.audio_media_id WHERE r.slug=?
                    """ + status, (rs, row) -> mapRule(rs), slug);
        } catch (EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_CONTENT_NOT_PUBLISHED",
                    "Rule not available", "The pronunciation rule is not published.");
        }
    }

    public PronunciationRuleView publicRule(String slug) {
        return ruleBySlug(slug, true);
    }

    private void validateRuleType(String type) {
        Set<String> allowed = Set.of("LINKING", "WEAK_FORM", "ASSIMILATION", "ELISION", "STRESS", "INTONATION");
        if (!allowed.contains(type)) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "ENGLISH_LISTENING_RULE_TYPE_INVALID", "Invalid rule type", "Unknown pronunciation rule type.");
    }

    private void assertRuleSlugFree(String slug, Long excludedId) {
        Integer c = excludedId == null
                ? jdbc.queryForObject("SELECT COUNT(*) FROM english_listening_pronunciation_rule WHERE slug=?",
                        Integer.class, slug)
                : jdbc.queryForObject("SELECT COUNT(*) FROM english_listening_pronunciation_rule WHERE slug=? AND id<>?",
                        Integer.class, slug, excludedId);
        if (c != null && c > 0) throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_CONTENT_SLUG_CONFLICT",
                "Slug already in use", "Choose another stable slug.");
    }

    private boolean ruleSlugExists(String slug, Long excludedId) {
        Integer count = excludedId == null
                ? jdbc.queryForObject("SELECT COUNT(*) FROM english_listening_pronunciation_rule WHERE slug=?",
                Integer.class, slug)
                : jdbc.queryForObject("SELECT COUNT(*) FROM english_listening_pronunciation_rule WHERE slug=? AND id<>?",
                Integer.class, slug, excludedId);
        return count != null && count > 0;
    }

    private Integer nextRuleSort() {
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_order),0) FROM english_listening_pronunciation_rule", Integer.class);
        return (max == null ? 0 : max) + 10;
    }

    private PronunciationRuleView mapRule(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        return new PronunciationRuleView(id,
                rs.getString("rule_type"), rs.getString("title"), rs.getString("slug"), rs.getString("summary"),
                rs.getString("body_markdown"), nullableLong(rs, "audio_media_id"), rs.getString("audio_url"),
                rs.getString("publish_status"), rs.getInt("sort_order"),
                format(rs.getTimestamp("published_at")), format(rs.getTimestamp("updated_at")), null, null);
    }
}
