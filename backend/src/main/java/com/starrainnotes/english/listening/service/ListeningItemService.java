package com.starrainnotes.english.listening.service;

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
import com.starrainnotes.english.listening.mapper.ListeningItemMapper;
import com.starrainnotes.english.shared.exercise.mapper.EnglishExerciseMapper;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Listening material domain (方案 §7, 阶段三 §四, §五).
 *
 * <p>The backend (never the client) validates audio is an AUDIO asset and the
 * cover is an IMAGE asset, respects the publish lifecycle, refuses to delete a
 * published item, and cleans up a draft item's exercises on delete. Segments
 * are time-range validated (end &gt; start, end ≤ audio duration unless 0) and
 * normalized to 10/20/30 via a two-phase temporary value.</p>
 */
@Service
public class ListeningItemService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String WITHDRAWN = "WITHDRAWN";
    private static final int MAX_PAGE_SIZE = 50;

    private static final String ITEM_SELECT = """
            SELECT i.id,i.title,i.slug,i.summary,i.transcript_markdown,i.cefr_level,i.listening_level,
                   i.audio_media_id,a.public_url AS audio_url,i.cover_media_id,c.public_url AS cover_url,
                   i.duration_seconds,i.source_name,i.source_url,i.copyright_note,i.publish_status,
                   i.sort_order,i.published_at,i.updated_at
            FROM english_listening_item i
            LEFT JOIN media_asset a ON a.id=i.audio_media_id
            LEFT JOIN media_asset c ON c.id=i.cover_media_id
            """;

    private final ListeningItemMapper mapper;
    private final JdbcTemplate jdbc;
    private final EnglishExerciseMapper exerciseMapper;
    private final SiteSettingsTimezone timezone;

    public ListeningItemService(ListeningItemMapper mapper, JdbcTemplate jdbc,
                                EnglishExerciseMapper exerciseMapper, SiteSettingsTimezone timezone) {
        this.mapper = mapper;
        this.jdbc = jdbc;
        this.exerciseMapper = exerciseMapper;
        this.timezone = timezone;
    }

    @Transactional
    public ListeningItemView create(ListeningItemRequest request) {
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> itemSlugExists(candidate, null));
        assertSlugFree(slug, null);
        validateCefr(request.cefrLevel());
        validateLevel(request.listeningLevel());
        validateAudio(request.audioMediaId());
        validateCover(request.coverMediaId());
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
        validateCefr(request.cefrLevel());
        validateLevel(request.listeningLevel());
        validateAudio(request.audioMediaId());
        validateCover(request.coverMediaId());
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
            return current.withReadingPairs(publishedReadingPairs(current.id())).withNav(prev, next);
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
        List<ListeningItemSummaryView> rows = jdbc.query(sql, params.toArray(), (rs, row) -> mapSummary(rs));
        List<Long> ids = rows.stream().map(ListeningItemSummaryView::id).toList();
        Map<Long, List<ListeningTagRef>> tags = loadTags(ids);
        Map<Long, Long> exCounts = countBy(ids, "english_listening_item_exercise", "listening_item_id");
        Map<Long, Long> segCounts = countBy(ids, "english_listening_segment", "listening_item_id");
        List<ListeningItemSummaryView> items = rows.stream()
                .map(r -> r.withTags(tags.getOrDefault(r.id(), List.of()))
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
        List<String> problems = publishProblems(item);
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
        ListeningItem item = require(id);
        if (DRAFT.equals(item.getPublishStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_INVALID_PUBLISH_TRANSITION",
                    "Invalid publish transition", "A draft item cannot be withdrawn.");
        }
        jdbc.update("UPDATE english_listening_item SET publish_status='WITHDRAWN' WHERE id=?", id);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        ListeningItem item = require(id);
        if (PUBLISHED.equals(item.getPublishStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_LISTENING_PUBLISHED_DELETE_FORBIDDEN",
                    "Published item cannot be deleted", "Withdraw the item before deleting it.");
        }
        // listening_item_exercise + segment cascade via FK; exercise rows themselves are removed explicitly
        List<Long> exerciseIds = jdbc.queryForList(
                "SELECT exercise_id FROM english_listening_item_exercise WHERE listening_item_id=?", Long.class, id);
        jdbc.update("DELETE FROM english_listening_item WHERE id=?", id);
        for (Long exId : exerciseIds) exerciseMapper.deleteById(exId);
    }

    @Transactional
    public void addReadingPair(Long itemId, ReadingPairRequest request) {
        require(itemId);
        requireReading(request.readingArticleId());
        validateRelationType(request.relationType());
        try {
            jdbc.update("INSERT INTO english_reading_listening_pair"
                    + "(reading_article_id,listening_item_id,relation_type,sort_order) VALUES (?,?,?,?)",
                    request.readingArticleId(), itemId, request.relationType(), 10);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_READING_LISTENING_PAIR_DUPLICATE",
                    "Pair already exists", "This reading–listening pair is already linked.");
        }
    }

    @Transactional
    public void removeReadingPair(Long itemId, Long readingId) {
        jdbc.update("DELETE FROM english_reading_listening_pair WHERE listening_item_id=? AND reading_article_id=?",
                itemId, readingId);
    }

    // ---------------------------------------------------------------
    // segments
    // ---------------------------------------------------------------

    public List<ListeningSegmentView> segments(Long itemId) {
        require(itemId);
        return jdbc.query("""
                SELECT id,listening_item_id,start_ms,end_ms,transcript_text,translation_text,sort_order,updated_at
                FROM english_listening_segment WHERE listening_item_id=? ORDER BY sort_order,id
                """, (rs, row) -> mapSegment(rs), itemId);
    }

    public List<ListeningSegmentView> publicSegments(Long itemId) {
        requirePublished(itemId);
        return segments(itemId);
    }

    @Transactional
    public ListeningSegmentView createSegment(Long itemId, ListeningSegmentRequest request) {
        require(itemId);
        assertSegmentRange(itemId, request.startMs(), request.endMs());
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_order),0) FROM english_listening_segment WHERE listening_item_id=?",
                Integer.class, itemId);
        int order = (max == null ? 0 : max) + 10;
        jdbc.update("INSERT INTO english_listening_segment"
                + "(listening_item_id,start_ms,end_ms,transcript_text,translation_text,sort_order)"
                + " VALUES (?,?,?,?,?,?)",
                itemId, request.startMs(), request.endMs(), request.transcriptText(),
                clean(request.translationText()), order);
        return segmentById(itemId, order);
    }

    @Transactional
    public ListeningSegmentView updateSegment(Long itemId, Long segmentId, ListeningSegmentRequest request) {
        requireSegment(itemId, segmentId);
        assertSegmentRange(itemId, request.startMs(), request.endMs());
        jdbc.update("UPDATE english_listening_segment SET start_ms=?, end_ms=?, transcript_text=?,"
                + " translation_text=? WHERE id=? AND listening_item_id=?",
                request.startMs(), request.endMs(), request.transcriptText(),
                clean(request.translationText()), segmentId, itemId);
        return segmentById(itemId, segmentSort(itemId, segmentId));
    }

    @Transactional
    public void deleteSegment(Long itemId, Long segmentId) {
        requireSegment(itemId, segmentId);
        jdbc.update("DELETE FROM english_listening_segment WHERE id=? AND listening_item_id=?", segmentId, itemId);
    }

    @Transactional
    public void moveSegment(Long itemId, Long segmentId, int targetIndex) {
        requireSegment(itemId, segmentId);
        List<Long> ids = new ArrayList<>(jdbc.queryForList(
                "SELECT id FROM english_listening_segment WHERE listening_item_id=? ORDER BY sort_order,id",
                Long.class, itemId));
        ids.remove(segmentId);
        ids.add(Math.min(Math.max(targetIndex, 0), ids.size()), segmentId);
        if (ids.isEmpty()) return;
        jdbc.update("UPDATE english_listening_segment SET sort_order=100000 WHERE listening_item_id=?", itemId);
        for (int i = 0; i < ids.size(); i++) {
            jdbc.update("UPDATE english_listening_segment SET sort_order=? WHERE id=?",
                    (i + 1) * 10, ids.get(i));
        }
    }

    @Transactional
    public List<ListeningSegmentView> batchSegments(Long itemId, List<ListeningSegmentRequest> requests) {
        require(itemId);
        Integer duration = jdbc.queryForObject(
                "SELECT duration_seconds FROM english_listening_item WHERE id=?", Integer.class, itemId);
        int previousEnd = -1;
        for (ListeningSegmentRequest req : requests) {
            assertSegmentRangeWithDuration(itemId, req.startMs(), req.endMs(),
                    duration == null ? null : duration);
            if (isBlank(req.transcriptText())) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_LISTENING_SEGMENT_TEXT_REQUIRED",
                        "Segment transcript required", "Each listening segment must contain transcript text.");
            }
            if (previousEnd > req.startMs()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_LISTENING_SEGMENT_OVERLAP",
                        "Segments overlap", "Listening segments must be ordered and cannot overlap.");
            }
            previousEnd = req.endMs();
        }
        // Batch save is a complete, ordered replacement. Validate every row first,
        // then replace inside this transaction so a failed insert restores the old set.
        jdbc.update("DELETE FROM english_listening_segment WHERE listening_item_id=?", itemId);
        int order = 10;
        for (ListeningSegmentRequest req : requests) {
            jdbc.update("INSERT INTO english_listening_segment"
                    + "(listening_item_id,start_ms,end_ms,transcript_text,translation_text,sort_order)"
                    + " VALUES (?,?,?,?,?,?)",
                    itemId, req.startMs(), req.endMs(), req.transcriptText(),
                    clean(req.translationText()), order);
            order += 10;
        }
        return segments(itemId);
    }

    // ---------------------------------------------------------------
    // pronunciation rules
    // ---------------------------------------------------------------

    public List<com.starrainnotes.english.listening.dto.PronunciationRuleView> pronunciationRules(boolean publishedOnly) {
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
    public com.starrainnotes.english.listening.dto.PronunciationRuleView createRule(
            com.starrainnotes.english.listening.dto.PronunciationRuleRequest request) {
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
    public com.starrainnotes.english.listening.dto.PronunciationRuleView updateRule(Long id,
            com.starrainnotes.english.listening.dto.PronunciationRuleRequest request) {
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
    public com.starrainnotes.english.listening.dto.PronunciationRuleView publishRule(Long id) {
        requireRule(id);
        jdbc.update("UPDATE english_listening_pronunciation_rule SET publish_status='PUBLISHED',"
                + " published_at=COALESCE(published_at, UTC_TIMESTAMP(6)) WHERE id=?", id);
        return ruleById(id, false);
    }

    @Transactional
    public com.starrainnotes.english.listening.dto.PronunciationRuleView withdrawRule(Long id) {
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
        if (found.isEmpty()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_NOT_FOUND",
                "Invalid taxonomy term", "The selected tag does not exist or is disabled.");
        if (!dimension.equals(found.get(0))) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "ENGLISH_TAXONOMY_DEPTH_INVALID", "Invalid tag dimension",
                "A " + dimension + " tag cannot reference the '" + found.get(0) + "' dimension.");
    }

    private List<String> publishProblems(ListeningItem item) {
        List<String> problems = new ArrayList<>();
        if (isBlank(item.getTitle())) problems.add("标题不能为空");
        if (isBlank(item.getSlug())) problems.add("slug 不能为空");
        if (isBlank(item.getSummary())) problems.add("摘要不能为空");
        if (item.getCefrLevel() == null) problems.add("CEFR 等级不能为空");
        if (item.getListeningLevel() == null || item.getListeningLevel() < 1 || item.getListeningLevel() > 3) {
            problems.add("能力层级必须为1/2/3");
        }
        if (item.getAudioMediaId() == null) problems.add("音频资源不能为空");
        if (!hasDimensionTag(item.getId() == null ? 0L : item.getId(), "SCENE")) {
            problems.add("至少需要一个场景(SCENE)标签");
        }
        if (!hasDimensionTag(item.getId() == null ? 0L : item.getId(), "FORMAT")) {
            problems.add("至少需要一个形式(FORMAT)标签");
        }
        if (!segmentsValid(item.getId() == null ? 0L : item.getId())) {
            problems.add("至少需要一个包含原文、时间有效且互不重叠的片段");
        }
        if (!hasPublishedExercise(item.getId() == null ? 0L : item.getId())) {
            problems.add("至少需要一道已发布听力练习");
        } else if (!publishedExercisesValid(item.getId() == null ? 0L : item.getId())) {
            problems.add("存在配置不合法的已发布练习");
        }
        return problems;
    }

    private boolean segmentsValid(Long itemId) {
        Integer duration = jdbc.queryForObject(
                "SELECT duration_seconds FROM english_listening_item WHERE id=?", Integer.class, itemId);
        List<ListeningSegmentView> ranges = jdbc.query("""
                SELECT id,listening_item_id,start_ms,end_ms,transcript_text,translation_text,sort_order,updated_at
                FROM english_listening_segment WHERE listening_item_id=? ORDER BY sort_order,id
                """, (rs, row) -> mapSegment(rs), itemId);
        if (ranges.isEmpty()) return false;
        int previousEnd = -1;
        for (ListeningSegmentView segment : ranges) {
            if (isBlank(segment.transcriptText()) || segment.startMs() < 0 || segment.endMs() <= segment.startMs()) {
                return false;
            }
            if (previousEnd > segment.startMs()) return false;
            if (duration != null && duration > 0 && segment.endMs() > duration * 1000) return false;
            previousEnd = segment.endMs();
        }
        return true;
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

    private void assertSegmentRange(Long itemId, Integer start, Integer end) {
        Integer duration = jdbc.queryForObject(
                "SELECT duration_seconds FROM english_listening_item WHERE id=?", Integer.class, itemId);
        assertSegmentRangeWithDuration(itemId, start, end, duration);
    }

    private void assertSegmentRangeWithDuration(Long itemId, Integer start, Integer end, Integer duration) {
        if (start == null || start < 0) throw segmentInvalid("start_ms 必须为非负");
        if (end == null || end <= start) throw segmentInvalid("end_ms 必须大于 start_ms");
        if (duration != null && duration > 0 && end > duration * 1000) {
            throw segmentInvalid("end_ms 不能超过音频时长");
        }
    }

    private ApiException segmentInvalid(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_LISTENING_SEGMENT_INVALID",
                "Invalid segment", detail);
    }

    private ListeningItem require(Long id) {
        ListeningItem item = mapper.selectById(id);
        if (item == null) throw itemNotFound();
        return item;
    }

    private com.starrainnotes.english.listening.dto.PronunciationRuleView requireRule(Long id) {
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

    public com.starrainnotes.english.listening.dto.PronunciationRuleView ruleById(Long id, boolean publishedOnly) {
        return requireRule(id);
    }

    private com.starrainnotes.english.listening.dto.PronunciationRuleView ruleBySlug(String slug, boolean publishedOnly) {
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

    public com.starrainnotes.english.listening.dto.PronunciationRuleView publicRule(String slug) {
        return ruleBySlug(slug, true);
    }

    public void requirePublished(Long itemId) {
        String status = jdbc.queryForObject(
                "SELECT publish_status FROM english_listening_item WHERE id=?", String.class, itemId);
        if (!"PUBLISHED".equals(status)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_CONTENT_NOT_PUBLISHED",
                    "Item not available", "The listening material is not published.");
        }
    }

    private void requireReading(Long readingId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_reading_article WHERE id=?", Integer.class, readingId);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_LISTENING_PAIR_READING_INVALID",
                    "Invalid reading article", "The reading article does not exist.");
        }
    }

    private void requireSegment(Long itemId, Long segmentId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_listening_segment WHERE id=? AND listening_item_id=?",
                Integer.class, segmentId, itemId);
        if (count == null || count == 0) throw segmentNotFound();
    }

    private void validateCefr(String cefr) {
        if (cefr == null) return;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_cefr_standard WHERE level=?", Integer.class, cefr);
        if (count == null || count == 0) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "ENGLISH_CEFR_INVALID", "Invalid CEFR level", "The selected CEFR level does not exist.");
    }

    private void validateLevel(Integer level) {
        if (level == null || level < 1 || level > 3) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "ENGLISH_LISTENING_LEVEL_INVALID", "Invalid listening level", "Level must be 1, 2 or 3.");
    }

    private void validateAudio(Long mediaId) {
        if (mediaId == null) return;
        if (!isMediaType(mediaId, "AUDIO")) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "ENGLISH_AUDIO_MEDIA_INVALID", "Invalid audio", "The selected audio must be an AUDIO asset.");
    }

    private void validateCover(Long mediaId) {
        if (mediaId == null) return;
        if (!isMediaType(mediaId, "IMAGE")) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_COVER_MEDIA", "Invalid cover", "The selected cover must be an IMAGE asset.");
    }

    private boolean isMediaType(Long mediaId, String type) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM media_asset WHERE id=? AND asset_type=?", Integer.class, mediaId, type);
        return count != null && count > 0;
    }

    private void validateRelationType(String type) {
        Set<String> allowed = Set.of("SAME_CONTENT", "SAME_TOPIC", "EXTENDED_TRAINING");
        if (!allowed.contains(type)) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "ENGLISH_LISTENING_PAIR_TYPE_INVALID", "Invalid relation type",
                "relationType must be one of SAME_CONTENT/SAME_TOPIC/EXTENDED_TRAINING.");
    }

    private void validateRuleType(String type) {
        Set<String> allowed = Set.of("LINKING", "WEAK_FORM", "ASSIMILATION", "ELISION", "STRESS", "INTONATION");
        if (!allowed.contains(type)) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "ENGLISH_LISTENING_RULE_TYPE_INVALID", "Invalid rule type", "Unknown pronunciation rule type.");
    }

    private void assertSlugFree(String slug, Long excludedId) {
        LambdaQueryWrapper<ListeningItem> w = new LambdaQueryWrapper<ListeningItem>().eq(ListeningItem::getSlug, slug);
        if (excludedId != null) w.ne(ListeningItem::getId, excludedId);
        Long c = mapper.selectCount(w);
        if (c != null && c > 0) throw slugConflict();
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

    private boolean itemSlugExists(String slug, Long excludedId) {
        LambdaQueryWrapper<ListeningItem> wrapper =
                new LambdaQueryWrapper<ListeningItem>().eq(ListeningItem::getSlug, slug);
        if (excludedId != null) wrapper.ne(ListeningItem::getId, excludedId);
        Long count = mapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private boolean ruleSlugExists(String slug, Long excludedId) {
        Integer count = excludedId == null
                ? jdbc.queryForObject("SELECT COUNT(*) FROM english_listening_pronunciation_rule WHERE slug=?",
                Integer.class, slug)
                : jdbc.queryForObject("SELECT COUNT(*) FROM english_listening_pronunciation_rule WHERE slug=? AND id<>?",
                Integer.class, slug, excludedId);
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

    private Integer nextRuleSort() {
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_order),0) FROM english_listening_pronunciation_rule", Integer.class);
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
        String in = String.join(",", ids.stream().map(String::valueOf).toList());
        Map<Long, List<ListeningTagRef>> result = new LinkedHashMap<>();
        jdbc.query("""
                SELECT it.listening_item_id, t.id, t.name, t.slug, t.dimension, it.tag_role
                FROM english_listening_item_tag it
                JOIN english_taxonomy_term t ON t.id=it.term_id
                WHERE it.listening_item_id IN (""" + in + ") ORDER BY t.dimension, t.sort_order, t.id",
                rs -> {
                    while (rs.next()) result.computeIfAbsent(rs.getLong("listening_item_id"),
                            k -> new ArrayList<>()).add(new ListeningTagRef(rs.getLong("id"),
                            rs.getString("name"), rs.getString("slug"), rs.getString("dimension"),
                            rs.getString("tag_role")));
                    return null;
                });
        return result;
    }

    private Map<Long, Long> countBy(List<Long> ids, String table, String column) {
        if (ids.isEmpty()) return Map.of();
        String in = String.join(",", ids.stream().map(String::valueOf).toList());
        Map<Long, Long> result = new LinkedHashMap<>();
        jdbc.query("SELECT " + column + ", COUNT(*) FROM " + table + " WHERE " + column + " IN (" + in
                + ") GROUP BY " + column,
                rs -> { while (rs.next()) result.put(rs.getLong(1), rs.getLong(2)); return null; });
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
        return " AND EXISTS (SELECT 1 FROM english_listening_item_tag it JOIN english_taxonomy_term tt"
                + " ON tt.id=it.term_id WHERE it.listening_item_id=i.id AND it.term_id=? AND tt.dimension='" + dim + "')";
    }

    private ListeningItemSummaryView mapSummary(ResultSet rs) throws SQLException {
        return new ListeningItemSummaryView(rs.getLong("id"), rs.getString("title"), rs.getString("slug"),
                rs.getString("summary"), rs.getString("cover_url"), rs.getString("cefr_level"),
                rs.getInt("listening_level"), rs.getInt("duration_seconds"), rs.getString("publish_status"),
                0, 0, format(rs.getTimestamp("updated_at")), List.of());
    }

    private ListeningItemView mapView(ResultSet rs, int row) throws SQLException {
        Long id = rs.getLong("id");
        List<ListeningSegmentView> segs = id == null ? List.of() : segments(id);
        return new ListeningItemView(id, rs.getString("title"), rs.getString("slug"), rs.getString("summary"),
                rs.getString("transcript_markdown"), rs.getString("cefr_level"), rs.getInt("listening_level"),
                nullableLong(rs, "audio_media_id"), rs.getString("audio_url"),
                nullableLong(rs, "cover_media_id"), rs.getString("cover_url"), rs.getInt("duration_seconds"),
                rs.getString("source_name"), rs.getString("source_url"), rs.getString("copyright_note"),
                rs.getString("publish_status"), rs.getInt("sort_order"),
                format(rs.getTimestamp("published_at")), format(rs.getTimestamp("updated_at")),
                loadTags(List.of(id)).getOrDefault(id, List.of()), segs, readingPairs(id), null, null);
    }

    public List<ReadingPairRef> readingPairs(Long itemId) {
        return jdbc.query("""
                SELECT rp.reading_article_id, ar.title, ar.slug, rp.relation_type
                FROM english_reading_listening_pair rp
                JOIN english_reading_article ar ON ar.id=rp.reading_article_id
                WHERE rp.listening_item_id=? ORDER BY rp.sort_order, ar.id
                """, (rs, row) -> new ReadingPairRef(rs.getLong("reading_article_id"),
                rs.getString("title"), rs.getString("slug"), rs.getString("relation_type")), itemId);
    }

    private List<ReadingPairRef> publishedReadingPairs(Long itemId) {
        return jdbc.query("""
                SELECT rp.reading_article_id, ar.title, ar.slug, rp.relation_type
                FROM english_reading_listening_pair rp
                JOIN english_reading_article ar ON ar.id=rp.reading_article_id
                WHERE rp.listening_item_id=? AND ar.publish_status='PUBLISHED'
                ORDER BY rp.sort_order, ar.id
                """, (rs, row) -> new ReadingPairRef(rs.getLong("reading_article_id"),
                rs.getString("title"), rs.getString("slug"), rs.getString("relation_type")), itemId);
    }

    private ListeningSegmentView segmentById(Long itemId, int sortOrder) {
        return jdbc.queryForObject("""
                SELECT id,listening_item_id,start_ms,end_ms,transcript_text,translation_text,sort_order,updated_at
                FROM english_listening_segment WHERE listening_item_id=? AND sort_order=?
                """, (rs, row) -> mapSegment(rs), itemId, sortOrder);
    }

    private Integer segmentSort(Long itemId, Long segmentId) {
        return jdbc.queryForObject(
                "SELECT sort_order FROM english_listening_segment WHERE id=? AND listening_item_id=?",
                Integer.class, segmentId, itemId);
    }

    private ListeningSegmentView mapSegment(ResultSet rs) throws SQLException {
        return new ListeningSegmentView(rs.getLong("id"), rs.getLong("listening_item_id"),
                rs.getInt("start_ms"), rs.getInt("end_ms"), rs.getString("transcript_text"),
                rs.getString("translation_text"), rs.getInt("sort_order"),
                format(rs.getTimestamp("updated_at")));
    }

    private com.starrainnotes.english.listening.dto.PronunciationRuleView mapRule(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        return new com.starrainnotes.english.listening.dto.PronunciationRuleView(id,
                rs.getString("rule_type"), rs.getString("title"), rs.getString("slug"), rs.getString("summary"),
                rs.getString("body_markdown"), nullableLong(rs, "audio_media_id"), rs.getString("audio_url"),
                rs.getString("publish_status"), rs.getInt("sort_order"),
                format(rs.getTimestamp("published_at")), format(rs.getTimestamp("updated_at")), null, null);
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
    private ApiException segmentNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_LISTENING_SEGMENT_NOT_FOUND",
                "Segment not found", "The segment does not exist for this item.");
    }
    private ApiException slugConflict() {
        return new ApiException(HttpStatus.CONFLICT, "ENGLISH_CONTENT_SLUG_CONFLICT",
                "Slug already in use", "Choose another stable slug.");
    }
}
