package com.starrainnotes.english.listening.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.english.listening.dto.ListeningAdminStats;
import com.starrainnotes.english.listening.dto.ListeningHomeView;
import com.starrainnotes.english.listening.dto.ListeningItemRequest;
import com.starrainnotes.english.listening.dto.ListeningItemSummaryView;
import com.starrainnotes.english.listening.dto.ListeningItemView;
import com.starrainnotes.english.listening.dto.ListeningLinkView;
import com.starrainnotes.english.listening.dto.ListeningPageView;
import com.starrainnotes.english.listening.dto.ListeningSegmentRequest;
import com.starrainnotes.english.listening.dto.ListeningSegmentView;
import com.starrainnotes.english.listening.dto.ListeningTagRef;
import com.starrainnotes.english.listening.dto.ReadingPairRef;
import com.starrainnotes.english.listening.dto.ReadingPairRequest;
import com.starrainnotes.english.listening.entity.ListeningItem;
import com.starrainnotes.english.listening.domain.ListeningContentPort;
import com.starrainnotes.english.shared.content.ContentDescriptor;
import com.starrainnotes.english.shared.content.ContentCatalogFilter;
import com.starrainnotes.english.shared.content.ContentCatalogSlice;
import com.starrainnotes.english.shared.content.EnglishContentType;
import com.starrainnotes.english.listening.domain.ListeningPublishPolicy;
import com.starrainnotes.english.listening.domain.SegmentTimelinePolicy;
import com.starrainnotes.english.listening.mapper.ListeningItemMapper;
import com.starrainnotes.english.shared.exercise.mapper.EnglishExerciseMapper;
import com.starrainnotes.english.api.MediaPort;
import com.starrainnotes.english.listening.infrastructure.ListeningRelationRepository;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listening material domain (方案 §7, 阶段三 §四, §五).
 *
 * <p>The backend (never the client) validates audio is an AUDIO asset and the
 * cover is an IMAGE asset, respects the publish lifecycle, refuses to delete a
 * published item, and cleans up a draft item's exercises on delete. Segments
 * are time-range validated (end &gt; start, end ≤ audio duration unless 0) and
 * normalized to 10/20/30 via a two-phase temporary value.</p>
 */
@Repository
@Transactional(readOnly = true)
public class ListeningRepository implements ListeningContentPort {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String WITHDRAWN = "WITHDRAWN";
    private static final int MAX_PAGE_SIZE = 50;

    private static final String ITEM_SELECT = """
            SELECT i.id,i.title,i.slug,i.summary,i.transcript_markdown,i.cefr_level,i.listening_level,
                   i.audio_media_id,i.cover_media_id,
                   i.duration_seconds,i.source_name,i.source_url,i.copyright_note,i.publish_status,
                   i.sort_order,i.published_at,i.updated_at
            FROM english_listening_item i
            """;

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
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM english_listening_item a" + where,
                Long.class, args.toArray());
        if (limit <= 0) return new ContentCatalogSlice(total == null ? 0 : total, List.of());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(limit);
        Map<Long, Long> coverIds = new HashMap<>();
        List<ContentDescriptor> items = jdbc.query("""
                SELECT a.id,a.slug,a.title,a.summary,a.cefr_level,a.cover_media_id,
                       a.publish_status,a.sort_order
                FROM english_listening_item a
                """ + where + " ORDER BY FIELD(a.publish_status,'PUBLISHED','DRAFT','WITHDRAWN'),a.sort_order,a.id LIMIT ?",
                (rs, row) -> {
                    long id = rs.getLong("id");
                    long coverId = rs.getLong("cover_media_id");
                    if (!rs.wasNull()) coverIds.put(id, coverId);
                    return new ContentDescriptor(EnglishContentType.LISTENING,
                            id, rs.getString("slug"), rs.getString("title"), rs.getString("summary"),
                            rs.getString("cefr_level"), null, rs.getString("publish_status"),
                            rs.getInt("sort_order"));
                }, pageArgs.toArray());
        Map<Long, String> coverUrls = mediaAssets.publicUrls(coverIds.values());
        return new ContentCatalogSlice(total == null ? 0 : total, items.stream()
                .map(item -> item.withCoverUrl(coverIds.containsKey(item.id())
                        ? coverUrls.get(coverIds.get(item.id())) : null)).toList());
    }

    private final ListeningItemMapper mapper;
    private final JdbcTemplate jdbc;
    private final EnglishExerciseMapper exerciseMapper;
    private final SiteSettingsTimezone timezone;
    private final ListeningSegmentRepository segmentRepository;
    private final MediaPort mediaAssets;
    private final ListeningRelationRepository relations;
    private final ListeningPublishPolicy publishPolicy;
    private final SegmentTimelinePolicy timelinePolicy;

    public ListeningRepository(ListeningItemMapper mapper, JdbcTemplate jdbc,
                                EnglishExerciseMapper exerciseMapper, SiteSettingsTimezone timezone,
                                ListeningSegmentRepository segmentRepository, MediaPort mediaAssets,
                                ListeningRelationRepository relations, ListeningPublishPolicy publishPolicy,
                                SegmentTimelinePolicy timelinePolicy) {
        this.mapper = mapper;
        this.jdbc = jdbc;
        this.exerciseMapper = exerciseMapper;
        this.timezone = timezone;
        this.segmentRepository = segmentRepository;
        this.mediaAssets = mediaAssets;
        this.relations = relations;
        this.publishPolicy = publishPolicy;
        this.timelinePolicy = timelinePolicy;
    }

    @Transactional
    public ListeningItemView create(ListeningItemRequest request) {
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> itemSlugExists(candidate, null));
        assertSlugFree(slug, null);
        ListeningItem item = new ListeningItem();
        applyFields(item, request, slug);
        item.setPublishStatus(DRAFT);
        if (item.getSortOrder() == null) item.setSortOrder(nextSort());
        item.setDurationSeconds(request.durationSeconds() == null ? 0 : request.durationSeconds());
        try {
            mapper.insert(item);
        } catch (DuplicateKeyException ex) {
            throw slugConflict();
        }
        replaceTags(item.getId(), request);
        return get(item.getId());
    }

    @Transactional
    public ListeningItemView update(Long id, ListeningItemRequest request) {
        ListeningItem item = require(id);
        String slug = NumericSlugGenerator.forUpdate(request.slug(), item.getSlug());
        assertSlugFree(slug, id);
        applyFields(item, request, slug);
        if (request.durationSeconds() != null) item.setDurationSeconds(request.durationSeconds());
        try {
            mapper.updateById(item);
        } catch (DuplicateKeyException ex) {
            throw slugConflict();
        }
        replaceTags(id, request);
        return get(id);
    }

    public ListeningItemView get(Long id) {
        try {
            return jdbc.queryForObject(ITEM_SELECT + " WHERE i.id=?", this::mapView, id);
        } catch (EmptyResultDataAccessException ex) {
            throw itemNotFound();
        }
    }

    public ListeningItemView publicGet(String slug) {
        try {
            ListeningItemView current = jdbc.queryForObject(
                    ITEM_SELECT + " WHERE i.slug=? AND i.publish_status='PUBLISHED'", this::mapView, slug);
            List<Long> ids = jdbc.queryForList("""
                    SELECT id FROM english_listening_item
                    WHERE publish_status='PUBLISHED' AND listening_level=?
                    ORDER BY sort_order, id
                    """, Long.class, current.listeningLevel());
            int idx = ids.indexOf(current.id());
            ListeningLinkView prev = idx > 0 ? link(ids.get(idx - 1)) : null;
            ListeningLinkView next = idx >= 0 && idx + 1 < ids.size() ? link(ids.get(idx + 1)) : null;
            return current.withReadingPairs(relations.publishedReadingPairs(current.id())).withNav(prev, next);
        } catch (EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_CONTENT_NOT_PUBLISHED",
                    "Item not available", "The requested listening material is not published.");
        }
    }

    public ListeningPageView list(int page, int pageSize, String q, String status, Integer level,
                                  String cefr, Long topic, Long scene, Long format) {
        return listInternal(page, pageSize, q, status, level, cefr, topic, scene, format, false);
    }

    public ListeningPageView publicList(int page, int pageSize, String q, Integer level, String cefr,
                                        Long topic, Long scene, Long format) {
        return listInternal(page, pageSize, q, null, level, cefr, topic, scene, format, true);
    }

    private ListeningPageView listInternal(int page, int pageSize, String q, String status, Integer level,
                                           String cefr, Long topic, Long scene, Long format,
                                           boolean publicOnly) {
        List<Object> params = new ArrayList<>();
        String where = buildWhere(q, status, level, cefr, topic, scene, format, params);
        if (publicOnly) where += " AND i.publish_status='PUBLISHED'";
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM english_listening_item i " + where,
                Long.class, params.toArray());
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        String sql = ITEM_SELECT + where + " ORDER BY i.sort_order, i.id LIMIT " + safeSize + " OFFSET "
                + ((safePage - 1) * safeSize);
        Map<Long, Long> coverIds = new HashMap<>();
        List<ListeningItemSummaryView> rows = jdbc.query(sql, params.toArray(), (rs, row) -> {
            long coverId = rs.getLong("cover_media_id");
            if (!rs.wasNull()) coverIds.put(rs.getLong("id"), coverId);
            return mapSummary(rs);
        });
        Map<Long, String> coverUrls = mediaAssets.publicUrls(coverIds.values());
        List<Long> ids = rows.stream().map(ListeningItemSummaryView::id).toList();
        Map<Long, List<ListeningTagRef>> tags = loadTags(ids);
        Map<Long, Long> exCounts = countBy(ids, "english_listening_item_exercise", "listening_item_id");
        Map<Long, Long> segCounts = countBy(ids, "english_listening_segment", "listening_item_id");
        List<ListeningItemSummaryView> items = rows.stream()
                .map(r -> r.withCoverUrl(coverIds.containsKey(r.id())
                                ? coverUrls.get(coverIds.get(r.id())) : null)
                        .withTags(tags.getOrDefault(r.id(), List.of()))
                        .withCounts(exCounts.getOrDefault(r.id(), 0L), segCounts.getOrDefault(r.id(), 0L)))
                .toList();
        int totalPages = total == 0 ? 0 : (int) ((total + safeSize - 1) / safeSize);
        ListeningAdminStats stats = adminStats();
        return new ListeningPageView(items, safePage, safeSize, total, totalPages, publicOnly ? null : stats);
    }

    public ListeningHomeView home() {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_listening_item WHERE publish_status='PUBLISHED'", Long.class);
        Map<Integer, Long> byLevel = new LinkedHashMap<>();
        jdbc.query("SELECT listening_level, COUNT(*) FROM english_listening_item WHERE publish_status='PUBLISHED'"
                + " GROUP BY listening_level ORDER BY listening_level",
                rs -> { while (rs.next()) byLevel.put(rs.getInt(1), rs.getLong(2)); return null; });
        Map<String, Long> byCefr = new LinkedHashMap<>();
        jdbc.query("SELECT cefr_level, COUNT(*) FROM english_listening_item WHERE publish_status='PUBLISHED'"
                + " GROUP BY cefr_level ORDER BY cefr_level",
                rs -> { while (rs.next()) byCefr.put(rs.getString(1), rs.getLong(2)); return null; });
        return new ListeningHomeView(nvl(total), byLevel, byCefr, filterTags("TOPIC"), filterTags("SCENE"), filterTags("FORMAT"));
    }

    @Transactional
    public ListeningItemView publish(Long id) {
        ListeningItem item = require(id);
        List<String> problems = publishPolicy.problems(item,
                hasDimensionTag(id, "SCENE"), hasDimensionTag(id, "FORMAT"), segmentsValid(id),
                hasPublishedExercise(id), publishedExercisesValid(id));
        if (!problems.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_LISTENING_PUBLISH_INVALID",
                    "Cannot publish", String.join("; ", problems));
        }
        jdbc.update("UPDATE english_listening_item SET publish_status='PUBLISHED',"
                + " published_at=COALESCE(published_at, UTC_TIMESTAMP(6)) WHERE id=?", id);
        return get(id);
    }

    @Transactional
    public ListeningItemView withdraw(Long id) {
        require(id);
        jdbc.update("UPDATE english_listening_item SET publish_status='WITHDRAWN' WHERE id=?", id);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        // listening_item_exercise + segment cascade via FK; exercise rows themselves are removed explicitly
        List<Long> exerciseIds = jdbc.queryForList(
                "SELECT exercise_id FROM english_listening_item_exercise WHERE listening_item_id=?", Long.class, id);
        jdbc.update("DELETE FROM english_listening_item WHERE id=?", id);
        for (Long exId : exerciseIds) exerciseMapper.deleteById(exId);
    }

    // ---------------------------------------------------------------
    // item relations and query helpers
    // ---------------------------------------------------------------















    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void replaceTags(Long itemId, ListeningItemRequest request) {
        jdbc.update("DELETE FROM english_listening_item_tag WHERE listening_item_id=?", itemId);
        boolean primary = false;
        for (Long termId : nonNull(request.sceneTagIds())) {
            requireEnabledTag(termId, "SCENE");
            jdbc.update("INSERT INTO english_listening_item_tag(listening_item_id,term_id,tag_role)"
                    + " VALUES (?,?,?)", itemId, termId, primary ? "PRIMARY" : "TAG");
            primary = true;
        }
        for (Long termId : nonNull(request.formatTagIds())) {
            requireEnabledTag(termId, "FORMAT");
            jdbc.update("INSERT INTO english_listening_item_tag(listening_item_id,term_id,tag_role)"
                    + " VALUES (?,?,?)", itemId, termId, "TAG");
        }
        for (Long termId : nonNull(request.topicTagIds())) {
            requireEnabledTag(termId, "TOPIC");
            jdbc.update("INSERT INTO english_listening_item_tag(listening_item_id,term_id,tag_role)"
                    + " VALUES (?,?,?)", itemId, termId, "TAG");
        }
        for (Long termId : nonNull(request.abilityTagIds())) {
            requireEnabledTag(termId, "ABILITY");
            jdbc.update("INSERT INTO english_listening_item_tag(listening_item_id,term_id,tag_role)"
                    + " VALUES (?,?,?)", itemId, termId, "TAG");
        }
        for (Long termId : nonNull(request.functionTagIds())) {
            requireEnabledTag(termId, "FUNCTION");
            jdbc.update("INSERT INTO english_listening_item_tag(listening_item_id,term_id,tag_role)"
                    + " VALUES (?,?,?)", itemId, termId, "TAG");
        }
    }

    private void requireEnabledTag(Long termId, String dimension) {
        List<String> found = jdbc.query(
                "SELECT dimension FROM english_taxonomy_term WHERE id=? AND enabled=1",
                (rs, row) -> rs.getString("dimension"), termId);
        publishPolicy.requireEnabledDimension(dimension, found.isEmpty() ? null : found.get(0));
    }


    /** Lightweight ownership check for child-resource endpoints. */
    @Override
    public void requireExists(Long id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_listening_item WHERE id=?", Integer.class, id);
        if (count == null || count == 0) throw itemNotFound();
    }

    private boolean segmentsValid(Long itemId) {
        Integer duration = jdbc.queryForObject(
                "SELECT duration_seconds FROM english_listening_item WHERE id=?", Integer.class, itemId);
        return timelinePolicy.isValid(segmentRepository.list(itemId), duration);
    }


    private boolean hasPublishedExercise(Long itemId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM english_listening_item_exercise lie
                JOIN english_exercise e ON e.id=lie.exercise_id
                WHERE lie.listening_item_id=? AND e.publish_status='PUBLISHED'
                """, Integer.class, itemId);
        return count != null && count > 0;
    }

    private boolean publishedExercisesValid(Long itemId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM english_listening_item_exercise lie
                JOIN english_exercise e ON e.id=lie.exercise_id
                WHERE lie.listening_item_id=? AND e.publish_status='PUBLISHED' AND e.config_json IS NULL
                """, Integer.class, itemId);
        return count == null || count == 0;
    }

    private boolean hasDimensionTag(Long itemId, String dimension) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM english_listening_item_tag it
                JOIN english_taxonomy_term t ON t.id=it.term_id
                WHERE it.listening_item_id=? AND t.dimension=? AND t.enabled=1
                """, Integer.class, itemId, dimension);
        return count != null && count > 0;
    }




    private ListeningItem require(Long id) {
        ListeningItem item = mapper.selectById(id);
        if (item == null) throw itemNotFound();
        return item;
    }









    @Override
    public void requirePublished(Long itemId) {
        String status = jdbc.queryForObject(
                "SELECT publish_status FROM english_listening_item WHERE id=?", String.class, itemId);
        if (!"PUBLISHED".equals(status)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_CONTENT_NOT_PUBLISHED",
                    "Item not available", "The listening material is not published.");
        }
    }



    public boolean cefrExists(String cefr) {
        if (cefr == null) return true;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_cefr_standard WHERE level=?", Integer.class, cefr);
        return count != null && count > 0;
    }




    private void assertSlugFree(String slug, Long excludedId) {
        LambdaQueryWrapper<ListeningItem> w = new LambdaQueryWrapper<ListeningItem>().eq(ListeningItem::getSlug, slug);
        if (excludedId != null) w.ne(ListeningItem::getId, excludedId);
        Long c = mapper.selectCount(w);
        if (c != null && c > 0) throw slugConflict();
    }



    private boolean itemSlugExists(String slug, Long excludedId) {
        LambdaQueryWrapper<ListeningItem> wrapper =
                new LambdaQueryWrapper<ListeningItem>().eq(ListeningItem::getSlug, slug);
        if (excludedId != null) wrapper.ne(ListeningItem::getId, excludedId);
        Long count = mapper.selectCount(wrapper);
        return count != null && count > 0;
    }



    private void applyFields(ListeningItem item, ListeningItemRequest request, String slug) {
        item.setTitle(request.title().trim());
        item.setSlug(slug);
        item.setSummary(request.summary().trim());
        item.setTranscriptMarkdown(clean(request.transcriptMarkdown()));
        item.setCefrLevel(request.cefrLevel());
        item.setListeningLevel(request.listeningLevel());
        item.setAudioMediaId(request.audioMediaId());
        item.setCoverMediaId(request.coverMediaId());
        item.setSourceName(clean(request.sourceName()));
        item.setSourceUrl(clean(request.sourceUrl()));
        item.setCopyrightNote(clean(request.copyrightNote()));
        if (request.sortOrder() != null) item.setSortOrder(request.sortOrder());
    }

    private Integer nextSort() {
        Integer max = jdbc.queryForObject("SELECT COALESCE(MAX(sort_order),0) FROM english_listening_item", Integer.class);
        return (max == null ? 0 : max) + 10;
    }



    private ListeningAdminStats adminStats() {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM english_listening_item", Long.class);
        Long published = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_listening_item WHERE publish_status='PUBLISHED'", Long.class);
        Long draft = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_listening_item WHERE publish_status='DRAFT'", Long.class);
        Long withdrawn = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_listening_item WHERE publish_status='WITHDRAWN'", Long.class);
        Long missing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_listening_item WHERE audio_media_id IS NULL", Long.class);
        Map<Integer, Long> byLevel = new LinkedHashMap<>();
        jdbc.query("SELECT listening_level, COUNT(*) FROM english_listening_item GROUP BY listening_level"
                + " ORDER BY listening_level",
                rs -> { while (rs.next()) byLevel.put(rs.getInt(1), rs.getLong(2)); return null; });
        return new ListeningAdminStats(nvl(total), nvl(published), nvl(draft), nvl(withdrawn), nvl(missing), byLevel);
    }

    private Map<Long, List<ListeningTagRef>> loadTags(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        Map<Long, List<ListeningTagRef>> result = new LinkedHashMap<>();
        jdbc.query("""
                SELECT it.listening_item_id, t.id, t.name, t.slug, t.dimension, it.tag_role
                FROM english_listening_item_tag it
                JOIN english_taxonomy_term t ON t.id=it.term_id
                WHERE it.listening_item_id IN (""" + placeholders + ") ORDER BY t.dimension, t.sort_order, t.id",
                rs -> {
                    while (rs.next()) result.computeIfAbsent(rs.getLong("listening_item_id"),
                            k -> new ArrayList<>()).add(new ListeningTagRef(rs.getLong("id"),
                            rs.getString("name"), rs.getString("slug"), rs.getString("dimension"),
                            rs.getString("tag_role")));
                    return null;
                }, ids.toArray());
        return result;
    }

    private Map<Long, Long> countBy(List<Long> ids, String table, String column) {
        if (ids.isEmpty()) return Map.of();
        String counted = switch (table + "." + column) {
            case "english_listening_item_exercise.listening_item_id" -> """
                    SELECT listening_item_id, COUNT(*) FROM english_listening_item_exercise
                    WHERE listening_item_id IN (%s) GROUP BY listening_item_id""";
            case "english_listening_segment.listening_item_id" -> """
                    SELECT listening_item_id, COUNT(*) FROM english_listening_segment
                    WHERE listening_item_id IN (%s) GROUP BY listening_item_id""";
            default -> throw new IllegalArgumentException("Unsupported count: " + table + "." + column);
        };
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        Map<Long, Long> result = new LinkedHashMap<>();
        jdbc.query(counted.formatted(placeholders), rs -> {
            while (rs.next()) result.put(rs.getLong(1), rs.getLong(2));
            return null;
        }, ids.toArray());
        return result;
    }

    private String buildWhere(String q, String status, Integer level, String cefr, Long topic, Long scene,
                              Long format, List<Object> params) {
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        if (q != null && !q.isBlank()) {
            String p = "%" + q.trim() + "%";
            w.append(" AND (i.title LIKE ? OR i.summary LIKE ?)");
            params.add(p); params.add(p);
        }
        if (status != null && !status.isBlank()) { w.append(" AND i.publish_status=?"); params.add(status); }
        if (level != null) { w.append(" AND i.listening_level=?"); params.add(level); }
        if (cefr != null && !cefr.isBlank()) { w.append(" AND i.cefr_level=?"); params.add(cefr); }
        if (topic != null) w.append(tagExists("TOPIC", topic, params));
        if (scene != null) w.append(tagExists("SCENE", scene, params));
        if (format != null) w.append(tagExists("FORMAT", format, params));
        return w.toString();
    }

    private String tagExists(String dim, Long termId, List<Object> params) {
        params.add(termId);
        params.add(dim);
        return " AND EXISTS (SELECT 1 FROM english_listening_item_tag it JOIN english_taxonomy_term tt"
                + " ON tt.id=it.term_id WHERE it.listening_item_id=i.id AND it.term_id=? AND tt.dimension=?)";
    }

    private ListeningItemSummaryView mapSummary(ResultSet rs) throws SQLException {
        return new ListeningItemSummaryView(rs.getLong("id"), rs.getString("title"), rs.getString("slug"),
                rs.getString("summary"), null, rs.getString("cefr_level"),
                rs.getInt("listening_level"), rs.getInt("duration_seconds"), rs.getString("publish_status"),
                0, 0, format(rs.getTimestamp("updated_at")), List.of());
    }

    private ListeningItemView mapView(ResultSet rs, int row) throws SQLException {
        Long id = rs.getLong("id");
        List<ListeningSegmentView> segs = id == null ? List.of() : segmentRepository.list(id);
        return new ListeningItemView(id, rs.getString("title"), rs.getString("slug"), rs.getString("summary"),
                rs.getString("transcript_markdown"), rs.getString("cefr_level"), rs.getInt("listening_level"),
                nullableLong(rs, "audio_media_id"), mediaAssets.publicUrl(nullableLong(rs, "audio_media_id")),
                nullableLong(rs, "cover_media_id"), mediaAssets.publicUrl(nullableLong(rs, "cover_media_id")),
                rs.getInt("duration_seconds"),
                rs.getString("source_name"), rs.getString("source_url"), rs.getString("copyright_note"),
                rs.getString("publish_status"), rs.getInt("sort_order"),
                format(rs.getTimestamp("published_at")), format(rs.getTimestamp("updated_at")),
                loadTags(List.of(id)).getOrDefault(id, List.of()), segs, relations.readingPairs(id), null, null);
    }








    private ListeningLinkView link(Long id) {
        return jdbc.queryForObject("SELECT title, slug FROM english_listening_item WHERE id=?",
                (rs, row) -> new ListeningLinkView(rs.getString("title"), rs.getString("slug")), id);
    }

    private List<ListeningTagRef> filterTags(String dimension) {
        return jdbc.query("""
                SELECT id,name,slug,dimension,'TAG' AS role FROM english_taxonomy_term
                WHERE dimension=? AND enabled=1 AND parent_id IS NULL ORDER BY sort_order,id
                """, (rs, row) -> new ListeningTagRef(rs.getLong("id"), rs.getString("name"),
                rs.getString("slug"), rs.getString("dimension"), rs.getString("role")), dimension);
    }

    private List<Long> nonNull(List<Long> ids) { return ids == null ? List.of() : ids; }
    private long nvl(Long v) { return v == null ? 0L : v; }
    private Long nullableLong(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col); return rs.wasNull() ? null : v;
    }
    private String format(java.sql.Timestamp ts) {
        return ts == null ? null : timezone.atSite(ts.toLocalDateTime()).format(ISO_OFFSET);
    }
    private String clean(String v) { return v == null ? null : (v.trim().isEmpty() ? null : v.trim()); }
    private boolean isBlank(String v) { return v == null || v.isBlank(); }
    private ApiException itemNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_LISTENING_ITEM_NOT_FOUND",
                "Item not found", "The listening material does not exist.");
    }
    private ApiException slugConflict() {
        return new ApiException(HttpStatus.CONFLICT, "ENGLISH_CONTENT_SLUG_CONFLICT",
                "Slug already in use", "Choose another stable slug.");
    }
}
