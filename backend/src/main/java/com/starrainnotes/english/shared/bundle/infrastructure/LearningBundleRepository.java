package com.starrainnotes.english.shared.bundle.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.english.shared.bundle.dto.BundleView;
import com.starrainnotes.english.shared.bundle.entity.EnglishLearningBundle;
import com.starrainnotes.english.shared.bundle.mapper.EnglishLearningBundleMapper;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.english.api.MediaPort;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
public class LearningBundleRepository {
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String BUNDLE_SELECT = """
            SELECT b.id,b.title,b.slug,b.summary,b.primary_cefr,b.cover_media_id,
                   b.publish_status,b.sort_order,b.published_at,b.updated_at
            FROM english_learning_bundle b
            """;

    private final EnglishLearningBundleMapper mapper;
    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;
    private final MediaPort media;

    public LearningBundleRepository(EnglishLearningBundleMapper mapper, JdbcTemplate jdbc,
                                    SiteSettingsTimezone timezone, MediaPort media) {
        this.mapper = mapper;
        this.jdbc = jdbc;
        this.timezone = timezone;
        this.media = media;
    }

    public List<BundleView> list() {
        return jdbc.query(BUNDLE_SELECT + " ORDER BY b.sort_order,b.id", this::mapView);
    }

    public List<BundleView> publishedList() {
        return jdbc.query(BUNDLE_SELECT + " WHERE b.publish_status='PUBLISHED' ORDER BY b.sort_order,b.id",
                this::mapView);
    }

    public BundleView publishedBySlug(String slug) {
        try {
            return jdbc.queryForObject(BUNDLE_SELECT + " WHERE b.slug=? AND b.publish_status='PUBLISHED'",
                    this::mapView, slug);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public BundleView detail(Long id) {
        try {
            return jdbc.queryForObject(BUNDLE_SELECT + " WHERE b.id=?", this::mapView, id);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public EnglishLearningBundle entity(Long id) { return mapper.selectById(id); }
    public void insert(EnglishLearningBundle bundle) { mapper.insert(bundle); }
    public void update(EnglishLearningBundle bundle) { mapper.updateById(bundle); }
    public void delete(Long id) { mapper.deleteById(id); }

    public void publish(Long id) {
        jdbc.update("""
                UPDATE english_learning_bundle
                SET publish_status='PUBLISHED', published_at=COALESCE(published_at,UTC_TIMESTAMP(6))
                WHERE id=?
                """, id);
    }

    public void withdraw(Long id) {
        jdbc.update("UPDATE english_learning_bundle SET publish_status='WITHDRAWN' WHERE id=?", id);
    }

    public int nextOrder() {
        Integer max = jdbc.queryForObject("SELECT COALESCE(MAX(sort_order),0) FROM english_learning_bundle",
                Integer.class);
        return (max == null ? 0 : max) + 10;
    }

    public boolean slugExists(String slug, Long excludedId) {
        LambdaQueryWrapper<EnglishLearningBundle> query =
                new LambdaQueryWrapper<EnglishLearningBundle>().eq(EnglishLearningBundle::getSlug, slug);
        if (excludedId != null) query.ne(EnglishLearningBundle::getId, excludedId);
        Long count = mapper.selectCount(query);
        return count != null && count > 0;
    }

    public boolean cefrExists(String cefr) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM english_cefr_standard WHERE level=?",
                Long.class, cefr);
        return count != null && count > 0;
    }

    private BundleView mapView(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new BundleView(rs.getLong("id"), rs.getString("title"), rs.getString("slug"),
                rs.getString("summary"), rs.getString("primary_cefr"),
                nullableLong(rs, "cover_media_id"), media.publicUrl(nullableLong(rs, "cover_media_id")),
                rs.getString("publish_status"),
                rs.getInt("sort_order"), format(rs.getTimestamp("published_at")),
                format(rs.getTimestamp("updated_at")));
    }

    private Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String format(java.sql.Timestamp timestamp) {
        if (timestamp == null) return null;
        return timezone.atSite(timestamp.toLocalDateTime()).format(ISO_OFFSET);
    }
}
