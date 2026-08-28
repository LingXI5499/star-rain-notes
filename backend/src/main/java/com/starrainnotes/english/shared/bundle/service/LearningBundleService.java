package com.starrainnotes.english.shared.bundle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.english.shared.bundle.dto.BundleRequest;
import com.starrainnotes.english.shared.bundle.dto.BundleView;
import com.starrainnotes.english.shared.bundle.entity.EnglishLearningBundle;
import com.starrainnotes.english.shared.bundle.mapper.EnglishLearningBundleMapper;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Learning bundle main-body management (方案 §9.2, §10.6, §11). Publishing and
 * withdrawing are explicit actions so the lifecycle (DRAFT/PUBLISHED/WITHDRAWN,
 * published_at consistency) is never bypassed by a generic save.
 */
@Service
public class LearningBundleService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String WITHDRAWN = "WITHDRAWN";

    private static final String BUNDLE_SELECT = """
            SELECT b.id,b.title,b.slug,b.summary,b.primary_cefr,b.cover_media_id,
                   m.public_url AS cover_url,b.publish_status,b.sort_order,b.published_at,b.updated_at
            FROM english_learning_bundle b
            LEFT JOIN media_asset m ON m.id=b.cover_media_id
            """;

    private final EnglishLearningBundleMapper mapper;
    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;
    private final LearningBundleItemService items;

    public LearningBundleService(EnglishLearningBundleMapper mapper,
                                 JdbcTemplate jdbc,
                                 SiteSettingsTimezone timezone,
                                 LearningBundleItemService items) {
        this.mapper = mapper;
        this.jdbc = jdbc;
        this.timezone = timezone;
        this.items = items;
    }

    public List<BundleView> list() {
        return jdbc.query(BUNDLE_SELECT + " ORDER BY b.sort_order, b.id", this::mapView);
    }

    public List<BundleView> publicList() {
        // Published legacy bundles remain visible even when they predate the
        // three-module readiness rule. The stricter rule is enforced only on
        // future publish/re-publish actions.
        return jdbc.query(BUNDLE_SELECT + " WHERE b.publish_status='PUBLISHED' ORDER BY b.sort_order, b.id",
                        this::mapView).stream()
                .filter(bundle -> items.publiclyAccessible(bundle.id()))
                .toList();
    }

    public BundleView get(Long id) {
        return requireDetail(id);
    }

    public BundleView publicGet(String slug) {
        try {
            BundleView bundle = jdbc.queryForObject(BUNDLE_SELECT + " WHERE b.slug=? AND b.publish_status='PUBLISHED'",
                    this::mapView, slug);
            if (bundle == null || !items.publiclyAccessible(bundle.id())) {
                throw contentNotPublished();
            }
            return bundle;
        } catch (EmptyResultDataAccessException ex) {
            throw contentNotPublished();
        }
    }

    @Transactional
    public BundleView create(BundleRequest request) {
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> slugExists(candidate, null));
        validateSlugFree(slug, null);
        validateCefr(request.primaryCefr());
        validateCover(request.coverMediaId());
        Integer order = request.sortOrder() == null ? nextOrder() : request.sortOrder();
        EnglishLearningBundle bundle = new EnglishLearningBundle();
        bundle.setTitle(request.title().trim());
        bundle.setSlug(slug);
        bundle.setSummary(clean(request.summary()));
        bundle.setPrimaryCefr(request.primaryCefr());
        bundle.setCoverMediaId(request.coverMediaId());
        bundle.setPublishStatus(DRAFT);
        bundle.setSortOrder(order);
        try {
            mapper.insert(bundle);
        } catch (DuplicateKeyException ex) {
            throw slugConflict();
        }
        return requireDetail(bundle.getId());
    }

    @Transactional
    public BundleView update(Long id, BundleRequest request) {
        EnglishLearningBundle bundle = requireEntity(id);
        String slug = NumericSlugGenerator.forUpdate(request.slug(), bundle.getSlug());
        validateSlugFree(slug, id);
        validateCefr(request.primaryCefr());
        validateCover(request.coverMediaId());
        bundle.setTitle(request.title().trim());
        bundle.setSlug(slug);
        bundle.setSummary(clean(request.summary()));
        bundle.setPrimaryCefr(request.primaryCefr());
        bundle.setCoverMediaId(request.coverMediaId());
        if (request.sortOrder() != null) {
            bundle.setSortOrder(request.sortOrder());
        }
        try {
            mapper.updateById(bundle);
        } catch (DuplicateKeyException ex) {
            throw slugConflict();
        }
        return requireDetail(id);
    }

    @Transactional
    public BundleView publish(Long id) {
        requireEntity(id);
        items.assertPublishable(id);
        jdbc.update("""
                UPDATE english_learning_bundle
                SET publish_status='PUBLISHED', published_at=COALESCE(published_at, UTC_TIMESTAMP(6))
                WHERE id=?
                """, id);
        return requireDetail(id);
    }

    @Transactional
    public BundleView withdraw(Long id) {
        String status = requireEntity(id).getPublishStatus();
        if (DRAFT.equals(status)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_INVALID_PUBLISH_TRANSITION",
                    "Invalid publish transition", "A draft bundle cannot be withdrawn.");
        }
        jdbc.update("UPDATE english_learning_bundle SET publish_status='WITHDRAWN' WHERE id=?", id);
        return requireDetail(id);
    }

    @Transactional
    public void delete(Long id) {
        requireEntity(id);
        mapper.deleteById(id);
    }

    private Integer nextOrder() {
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_order),0) FROM english_learning_bundle", Integer.class);
        return (max == null ? 0 : max) + 10;
    }

    private boolean slugExists(String slug, Long excludedId) {
        LambdaQueryWrapper<EnglishLearningBundle> wrapper =
                new LambdaQueryWrapper<EnglishLearningBundle>().eq(EnglishLearningBundle::getSlug, slug);
        if (excludedId != null) wrapper.ne(EnglishLearningBundle::getId, excludedId);
        Long count = mapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private void validateSlugFree(String slug, Long excludedId) {
        LambdaQueryWrapper<EnglishLearningBundle> wrapper = new LambdaQueryWrapper<EnglishLearningBundle>()
                .eq(EnglishLearningBundle::getSlug, slug);
        if (excludedId != null) {
            wrapper.ne(EnglishLearningBundle::getId, excludedId);
        }
        Long count = mapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw slugConflict();
        }
    }

    private void validateCefr(String cefr) {
        if (cefr == null) return;
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_cefr_standard WHERE level=?", Long.class, cefr);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_CEFR_INVALID",
                    "Invalid CEFR level", "The selected CEFR level does not exist.");
        }
    }

    private void validateCover(Long mediaId) {
        if (mediaId == null) return;
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM media_asset WHERE id=? AND asset_type='IMAGE'", Long.class, mediaId);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_COVER_MEDIA",
                    "Invalid cover", "The selected cover must be an existing image asset.");
        }
    }

    private BundleView requireDetail(Long id) {
        try {
            return jdbc.queryForObject(BUNDLE_SELECT + " WHERE b.id=?", this::mapView, id);
        } catch (EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_BUNDLE_NOT_FOUND",
                    "Bundle not found", "The learning bundle does not exist.");
        }
    }

    private EnglishLearningBundle requireEntity(Long id) {
        EnglishLearningBundle bundle = mapper.selectById(id);
        if (bundle == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_BUNDLE_NOT_FOUND",
                    "Bundle not found", "The learning bundle does not exist.");
        }
        return bundle;
    }

    private ApiException slugConflict() {
        return new ApiException(HttpStatus.CONFLICT, "ENGLISH_CONTENT_SLUG_CONFLICT",
                "Slug already in use", "Choose another stable slug.");
    }

    private ApiException contentNotPublished() {
        return new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_CONTENT_NOT_PUBLISHED",
                "Bundle not available", "The requested learning bundle is not published or is incomplete.");
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BundleView mapView(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new BundleView(rs.getLong("id"), rs.getString("title"), rs.getString("slug"),
                rs.getString("summary"), rs.getString("primary_cefr"),
                nullableLong(rs, "cover_media_id"), rs.getString("cover_url"), rs.getString("publish_status"),
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
