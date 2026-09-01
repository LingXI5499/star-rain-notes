package com.starrainnotes.english.grammar.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.english.grammar.dto.GrammarCourseView;
import com.starrainnotes.english.grammar.dto.GrammarCurriculumView;
import com.starrainnotes.english.grammar.dto.GrammarLessonDetailView;
import com.starrainnotes.english.grammar.dto.GrammarLessonLinkView;
import com.starrainnotes.english.grammar.dto.GrammarLessonRequest;
import com.starrainnotes.english.grammar.dto.GrammarLessonSummaryView;
import com.starrainnotes.english.grammar.dto.GrammarMoveRequest;
import com.starrainnotes.english.grammar.dto.GrammarReassignRequest;
import com.starrainnotes.english.grammar.dto.GrammarSectionRequest;
import com.starrainnotes.english.grammar.dto.GrammarSectionView;
import com.starrainnotes.english.grammar.dto.UpdateGrammarCourseRequest;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.seo.SeoContentChange;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EnglishGrammarService {

    private static final long COURSE_ID = 1L;
    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";
    private static final String WITHDRAWN = "WITHDRAWN";
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;

    public EnglishGrammarService(JdbcTemplate jdbc, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.timezone = timezone;
    }

    public GrammarCourseView course() {
        return requireCourse(false);
    }

    public GrammarCourseView publicCourse() {
        return requireCourse(true);
    }

    @Transactional
    public GrammarCourseView updateCourse(UpdateGrammarCourseRequest request) {
        requireCourse(false);
        validateCover(request.coverMediaId());
        jdbc.update("""
                UPDATE english_grammar_course
                SET title=?, subtitle=?, summary=?, introduction=?, roadmap_markdown=?, cover_media_id=?
                WHERE id=1
                """, request.title().trim(), clean(request.subtitle()), clean(request.summary()),
                clean(request.introduction()), clean(request.roadmapMarkdown()), request.coverMediaId());
        return requireCourse(false);
    }

    @Transactional
    public GrammarCourseView publishCourse() {
        requireCourse(false);
        jdbc.update("""
                UPDATE english_grammar_course
                SET publish_status='PUBLISHED', published_at=COALESCE(published_at, UTC_TIMESTAMP(6))
                WHERE id=1
                """);
        return requireCourse(false);
    }

    @Transactional
    public GrammarCourseView withdrawCourse() {
        GrammarCourseView course = requireCourse(false);
        if (DRAFT.equals(course.publishStatus())) {
            throw invalidTransition("A draft grammar course cannot be withdrawn.");
        }
        jdbc.update("UPDATE english_grammar_course SET publish_status='WITHDRAWN' WHERE id=1");
        return requireCourse(false);
    }

    public GrammarCurriculumView curriculum() {
        return buildCurriculum(false);
    }

    public GrammarCurriculumView publicCurriculum() {
        return buildCurriculum(true);
    }

    @Transactional
    public GrammarSectionView createSection(GrammarSectionRequest request) {
        requireCourse(false);
        Integer order = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_order),0)+10 FROM english_grammar_section WHERE course_id=1",
                Integer.class);
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO english_grammar_section(course_id,title,sort_order) VALUES (1,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.title().trim());
            statement.setInt(2, order == null ? 10 : order);
            return statement;
        }, keys);
        return sectionById(keys.getKey().longValue(), false);
    }

    @Transactional
    public GrammarSectionView updateSection(long sectionId, GrammarSectionRequest request) {
        requireSection(sectionId);
        jdbc.update("UPDATE english_grammar_section SET title=? WHERE id=? AND course_id=1",
                request.title().trim(), sectionId);
        return sectionById(sectionId, false);
    }

    @Transactional
    public void deleteSection(long sectionId) {
        requireSection(sectionId);
        Long lessons = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_grammar_lesson WHERE section_id=?", Long.class, sectionId);
        if (lessons != null && lessons > 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GRAMMAR_SECTION_NOT_EMPTY",
                    "Section is not empty", "Move or delete all lessons before deleting this section.");
        }
        jdbc.update("DELETE FROM english_grammar_section WHERE id=? AND course_id=1", sectionId);
        normalizeSections(loadSectionIds());
    }

    @Transactional
    public void moveSection(long sectionId, GrammarMoveRequest request) {
        requireSection(sectionId);
        List<Long> ids = loadSectionIds();
        ids.remove(sectionId);
        ids.add(Math.min(request.targetIndex(), ids.size()), sectionId);
        normalizeSections(ids);
    }

    @Transactional
    public GrammarLessonDetailView createLesson(GrammarLessonRequest request) {
        requireSection(request.sectionId());
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> slugExists(candidate, null));
        assertSlugFree(slug, null);
        Integer order = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_order),0)+10 FROM english_grammar_lesson WHERE section_id=?",
                Integer.class, request.sectionId());
        KeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO english_grammar_lesson
                            (course_id,section_id,title,slug,summary,body_markdown,publish_status,sort_order)
                        VALUES (1,?,?,?,?,?,'DRAFT',?)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, request.sectionId());
                statement.setString(2, request.title().trim());
                statement.setString(3, slug);
                statement.setString(4, clean(request.summary()));
                statement.setString(5, request.bodyMarkdown());
                statement.setInt(6, order == null ? 10 : order);
                return statement;
            }, keys);
        } catch (DuplicateKeyException ex) {
            throw slugConflict();
        }
        return lesson(keys.getKey().longValue());
    }

    public GrammarLessonDetailView lesson(long lessonId) {
        return requireLesson(lessonId, false);
    }

    @Transactional
    @SeoContentChange(table = "english_grammar_lesson", pathPrefix = "/english/grammar/")
    public GrammarLessonDetailView updateLesson(long lessonId, GrammarLessonRequest request) {
        LessonRow current = lessonRow(lessonId, false);
        if (!current.sectionId().equals(request.sectionId())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GRAMMAR_CROSS_SECTION_EDIT_FORBIDDEN",
                    "Use the move action", "A lesson can change sections only through the explicit reassign endpoint.");
        }
        String slug = NumericSlugGenerator.forUpdate(request.slug(), current.slug());
        assertSlugFree(slug, lessonId);
        try {
            jdbc.update("""
                    UPDATE english_grammar_lesson
                    SET title=?, slug=?, summary=?, body_markdown=?
                    WHERE id=? AND course_id=1
                    """, request.title().trim(), slug, clean(request.summary()),
                    request.bodyMarkdown(), lessonId);
        } catch (DuplicateKeyException ex) {
            throw slugConflict();
        }
        return lesson(lessonId);
    }

    @Transactional
    public void deleteLesson(long lessonId) {
        LessonRow lesson = lessonRow(lessonId, false);
        jdbc.update("DELETE FROM english_grammar_lesson WHERE id=? AND course_id=1", lessonId);
        normalizeLessons(lesson.sectionId(), loadLessonIds(lesson.sectionId()));
    }

    @Transactional
    @SeoContentChange(table = "english_grammar_lesson", pathPrefix = "/english/grammar/")
    public GrammarLessonDetailView publishLesson(long lessonId) {
        requireCourse(false);
        lessonRow(lessonId, false);
        jdbc.update("""
                UPDATE english_grammar_lesson
                SET publish_status='PUBLISHED', published_at=COALESCE(published_at, UTC_TIMESTAMP(6))
                WHERE id=? AND course_id=1
                """, lessonId);
        return lesson(lessonId);
    }

    @Transactional
    @SeoContentChange(table = "english_grammar_lesson", pathPrefix = "/english/grammar/")
    public GrammarLessonDetailView withdrawLesson(long lessonId) {
        LessonRow lesson = lessonRow(lessonId, false);
        if (DRAFT.equals(lesson.publishStatus())) {
            throw invalidTransition("A draft grammar lesson cannot be withdrawn.");
        }
        jdbc.update("UPDATE english_grammar_lesson SET publish_status='WITHDRAWN' WHERE id=?", lessonId);
        return this.lesson(lessonId);
    }

    @Transactional
    public void moveLesson(long lessonId, GrammarMoveRequest request) {
        LessonRow lesson = lessonRow(lessonId, false);
        List<Long> ids = loadLessonIds(lesson.sectionId());
        ids.remove(lessonId);
        ids.add(Math.min(request.targetIndex(), ids.size()), lessonId);
        normalizeLessons(lesson.sectionId(), ids);
    }

    @Transactional
    public void reassignLesson(long lessonId, GrammarReassignRequest request) {
        LessonRow lesson = lessonRow(lessonId, false);
        requireSection(request.targetSectionId());
        if (lesson.sectionId().equals(request.targetSectionId())) return;

        long sourceId = lesson.sectionId();
        List<Long> source = loadLessonIds(sourceId);
        source.remove(lessonId);
        normalizeLessons(sourceId, source);

        List<Long> target = loadLessonIds(request.targetSectionId());
        jdbc.update("UPDATE english_grammar_lesson SET section_id=?, sort_order=? WHERE id=?",
                request.targetSectionId(), (target.size() + 1) * 10, lessonId);
        target.add(lessonId);
        normalizeLessons(request.targetSectionId(), target);
    }

    public GrammarLessonDetailView publicLesson(String slug) {
        requireCourse(true);
        LessonRow current;
        try {
            current = jdbc.queryForObject(LESSON_SELECT +
                            " WHERE l.slug=? AND l.course_id=1 AND l.publish_status='PUBLISHED'",
                    this::mapLesson, slug);
        } catch (EmptyResultDataAccessException ex) {
            throw lessonNotFound();
        }
        List<LessonRow> published = jdbc.query(LESSON_SELECT + """
                WHERE l.course_id=1 AND l.publish_status='PUBLISHED'
                ORDER BY s.sort_order, s.id, l.sort_order, l.id
                """, this::mapLesson);
        int index = -1;
        for (int i = 0; i < published.size(); i++) {
            if (published.get(i).id().equals(current.id())) index = i;
        }
        GrammarLessonLinkView previous = index > 0 ? link(published.get(index - 1)) : null;
        GrammarLessonLinkView next = index >= 0 && index + 1 < published.size() ? link(published.get(index + 1)) : null;
        return toDetail(current, previous, next);
    }

    private GrammarCurriculumView buildCurriculum(boolean publicOnly) {
        GrammarCourseView course = requireCourse(publicOnly);
        String status = publicOnly ? " AND l.publish_status='PUBLISHED'" : "";
        List<LessonRow> lessons = jdbc.query(LESSON_SELECT +
                " WHERE l.course_id=1" + status + " ORDER BY s.sort_order,s.id,l.sort_order,l.id", this::mapLesson);
        Map<Long, List<LessonRow>> bySection = lessons.stream().collect(Collectors.groupingBy(LessonRow::sectionId));
        List<SectionRow> sections = jdbc.query("""
                SELECT id,title,sort_order FROM english_grammar_section
                WHERE course_id=1 ORDER BY sort_order,id
                """, (rs, row) -> new SectionRow(rs.getLong("id"), rs.getString("title"), rs.getInt("sort_order")));
        List<GrammarSectionView> views = new ArrayList<>();
        for (SectionRow section : sections) {
            List<LessonRow> rows = bySection.getOrDefault(section.id(), List.of());
            if (publicOnly && rows.isEmpty()) continue;
            List<GrammarLessonSummaryView> summaries = rows.stream().map(this::toSummary).toList();
            long published = rows.stream().filter(row -> PUBLISHED.equals(row.publishStatus())).count();
            views.add(new GrammarSectionView(section.id(), section.title(), section.sortOrder(),
                    summaries.size(), published, summaries));
        }
        return new GrammarCurriculumView(course, views);
    }

    private GrammarSectionView sectionById(long sectionId, boolean publicOnly) {
        return buildCurriculum(publicOnly).sections().stream().filter(s -> s.id().equals(sectionId))
                .findFirst().orElseThrow(this::sectionNotFound);
    }

    private GrammarLessonDetailView requireLesson(long lessonId, boolean publishedOnly) {
        return toDetail(lessonRow(lessonId, publishedOnly), null, null);
    }

    private LessonRow lessonRow(long lessonId, boolean publishedOnly) {
        try {
            String status = publishedOnly ? " AND l.publish_status='PUBLISHED'" : "";
            return jdbc.queryForObject(LESSON_SELECT + " WHERE l.id=? AND l.course_id=1" + status,
                    this::mapLesson, lessonId);
        } catch (EmptyResultDataAccessException ex) {
            throw lessonNotFound();
        }
    }

    private GrammarCourseView requireCourse(boolean publishedOnly) {
        try {
            String status = publishedOnly ? " AND c.publish_status='PUBLISHED'" : "";
            return jdbc.queryForObject("""
                    SELECT c.*,m.public_url AS cover_url FROM english_grammar_course c
                    LEFT JOIN media_asset m ON m.id=c.cover_media_id
                    WHERE c.id=1
                    """ + status, (rs, row) -> new GrammarCourseView(
                    rs.getLong("id"), rs.getString("title"), rs.getString("subtitle"),
                    rs.getString("summary"), rs.getString("introduction"), rs.getString("roadmap_markdown"),
                    nullableLong(rs, "cover_media_id"), rs.getString("cover_url"), rs.getString("seo_title"),
                    rs.getString("seo_description"), rs.getString("publish_status"),
                    format(rs.getTimestamp("published_at")), format(rs.getTimestamp("updated_at"))));
        } catch (EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "GRAMMAR_COURSE_NOT_FOUND",
                    "Grammar course not found", "The published English grammar course is not available.");
        }
    }

    private SectionRow requireSection(long sectionId) {
        try {
            return jdbc.queryForObject("""
                    SELECT id,title,sort_order FROM english_grammar_section WHERE id=? AND course_id=1
                    """, (rs, row) -> new SectionRow(rs.getLong("id"), rs.getString("title"),
                    rs.getInt("sort_order")), sectionId);
        } catch (EmptyResultDataAccessException ex) {
            throw sectionNotFound();
        }
    }

    private void assertSlugFree(String slug, Long excludedId) {
        Long count = excludedId == null
                ? jdbc.queryForObject("SELECT COUNT(*) FROM english_grammar_lesson WHERE slug=?", Long.class, slug)
                : jdbc.queryForObject("SELECT COUNT(*) FROM english_grammar_lesson WHERE slug=? AND id<>?",
                Long.class, slug, excludedId);
        if (count != null && count > 0) throw slugConflict();
    }

    private boolean slugExists(String slug, Long excludedId) {
        Long count = excludedId == null
                ? jdbc.queryForObject("SELECT COUNT(*) FROM english_grammar_lesson WHERE slug=?", Long.class, slug)
                : jdbc.queryForObject("SELECT COUNT(*) FROM english_grammar_lesson WHERE slug=? AND id<>?",
                Long.class, slug, excludedId);
        return count != null && count > 0;
    }

    private void validateCover(Long mediaId) {
        if (mediaId == null) return;
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM media_asset WHERE id=? AND asset_type='IMAGE'", Long.class, mediaId);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_COVER_MEDIA",
                    "Invalid grammar cover", "The selected cover must be an existing image asset.");
        }
    }

    private List<Long> loadSectionIds() {
        return jdbc.queryForList("""
                SELECT id FROM english_grammar_section WHERE course_id=1 ORDER BY sort_order,id
                """, Long.class);
    }

    private List<Long> loadLessonIds(long sectionId) {
        return jdbc.queryForList("""
                SELECT id FROM english_grammar_lesson WHERE section_id=? ORDER BY sort_order,id
                """, Long.class, sectionId);
    }

    private void normalizeSections(List<Long> ids) {
        if (ids.isEmpty()) return;
        jdbc.update("UPDATE english_grammar_section SET sort_order=100000+id WHERE course_id=1");
        for (int index = 0; index < ids.size(); index++) {
            jdbc.update("UPDATE english_grammar_section SET sort_order=? WHERE id=?",
                    (index + 1) * 10, ids.get(index));
        }
    }

    private void normalizeLessons(long sectionId, List<Long> ids) {
        if (ids.isEmpty()) return;
        jdbc.update("UPDATE english_grammar_lesson SET sort_order=100000+id WHERE section_id=?", sectionId);
        for (int index = 0; index < ids.size(); index++) {
            jdbc.update("UPDATE english_grammar_lesson SET sort_order=? WHERE id=? AND section_id=?",
                    (index + 1) * 10, ids.get(index), sectionId);
        }
    }

    private LessonRow mapLesson(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new LessonRow(rs.getLong("id"), rs.getLong("section_id"), rs.getString("section_title"),
                rs.getString("title"), rs.getString("slug"), rs.getString("summary"),
                rs.getString("body_markdown"), rs.getString("publish_status"), rs.getInt("sort_order"),
                rs.getTimestamp("published_at"), rs.getTimestamp("updated_at"));
    }

    private GrammarLessonSummaryView toSummary(LessonRow row) {
        return new GrammarLessonSummaryView(row.id(), row.sectionId(), row.title(), row.slug(), row.summary(),
                row.publishStatus(), row.sortOrder(), format(row.updatedAt()));
    }

    private GrammarLessonDetailView toDetail(LessonRow row, GrammarLessonLinkView previous,
                                              GrammarLessonLinkView next) {
        return new GrammarLessonDetailView(row.id(), row.sectionId(), row.sectionTitle(), row.title(),
                row.slug(), row.summary(), row.bodyMarkdown(), row.publishStatus(), row.sortOrder(),
                format(row.publishedAt()), format(row.updatedAt()), previous, next);
    }

    private GrammarLessonLinkView link(LessonRow row) {
        return new GrammarLessonLinkView(row.title(), row.slug());
    }

    private String format(Timestamp timestamp) {
        if (timestamp == null) return null;
        return timezone.atSite(timestamp.toLocalDateTime()).format(ISO_OFFSET);
    }

    private Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ApiException sectionNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "GRAMMAR_SECTION_NOT_FOUND",
                "Grammar section not found", "The grammar section does not exist.");
    }

    private ApiException lessonNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "GRAMMAR_LESSON_NOT_FOUND",
                "Grammar lesson not found", "The grammar lesson does not exist or is not published.");
    }

    private ApiException slugConflict() {
        return new ApiException(HttpStatus.CONFLICT, "GRAMMAR_LESSON_SLUG_CONFLICT",
                "Grammar lesson slug already exists", "Choose another stable lesson number.");
    }

    private ApiException invalidTransition(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PUBLISH_TRANSITION",
                "Invalid publish transition", detail);
    }

    private static final String LESSON_SELECT = """
            SELECT l.id,l.section_id,s.title AS section_title,l.title,l.slug,l.summary,l.body_markdown,
                   l.publish_status,l.sort_order,l.published_at,l.updated_at
            FROM english_grammar_lesson l
            JOIN english_grammar_section s ON s.id=l.section_id AND s.course_id=l.course_id
            """;

    private record SectionRow(Long id, String title, Integer sortOrder) { }

    private record LessonRow(Long id, Long sectionId, String sectionTitle, String title, String slug,
                             String summary, String bodyMarkdown, String publishStatus, Integer sortOrder,
                             Timestamp publishedAt, Timestamp updatedAt) { }
}
