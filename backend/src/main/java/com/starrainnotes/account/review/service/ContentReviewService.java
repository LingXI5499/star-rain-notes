package com.starrainnotes.account.review.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.audit.AuditLogService;
import com.starrainnotes.account.review.dto.ContentReviewView;
import com.starrainnotes.blog.dto.UpdatePostRequest;
import com.starrainnotes.blog.service.BlogService;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.grammar.dto.GrammarLessonRequest;
import com.starrainnotes.english.grammar.service.EnglishGrammarService;
import com.starrainnotes.english.listening.dto.ListeningItemRequest;
import com.starrainnotes.english.listening.dto.PronunciationRuleRequest;
import com.starrainnotes.english.listening.service.ListeningItemService;
import com.starrainnotes.english.reading.dto.ReadingArticleRequest;
import com.starrainnotes.english.reading.service.ReadingArticleService;
import com.starrainnotes.english.writing.dto.WritingPromptRequest;
import com.starrainnotes.english.writing.dto.WritingResourceRequest;
import com.starrainnotes.english.writing.service.WritingPromptService;
import com.starrainnotes.english.writing.service.WritingResourceService;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.tutorial.dto.UpdateChapterRequest;
import com.starrainnotes.tutorial.service.TutorialNodeService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ContentReviewService {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final BlogService blogService;
    private final TutorialNodeService tutorialNodeService;
    private final EnglishGrammarService grammarService;
    private final ReadingArticleService readingService;
    private final ListeningItemService listeningService;
    private final WritingResourceService writingResourceService;
    private final WritingPromptService writingPromptService;
    private final AuditLogService auditLogService;
    private final SiteSettingsTimezone timezone;

    public ContentReviewService(JdbcTemplate jdbc, ObjectMapper json, BlogService blogService,
                                TutorialNodeService tutorialNodeService,
                                EnglishGrammarService grammarService,
                                ReadingArticleService readingService,
                                ListeningItemService listeningService,
                                WritingResourceService writingResourceService,
                                WritingPromptService writingPromptService,
                                AuditLogService auditLogService, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.json = json;
        this.blogService = blogService;
        this.tutorialNodeService = tutorialNodeService;
        this.grammarService = grammarService;
        this.readingService = readingService;
        this.listeningService = listeningService;
        this.writingResourceService = writingResourceService;
        this.writingPromptService = writingPromptService;
        this.auditLogService = auditLogService;
        this.timezone = timezone;
    }

    public List<ContentReviewView> list(String status, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        String where = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status) ? "" : " WHERE status=?";
        Object[] args = where.isEmpty()
                ? new Object[]{safeSize, (safePage - 1) * safeSize}
                : new Object[]{status.toUpperCase(), safeSize, (safePage - 1) * safeSize};
        return jdbc.query("SELECT * FROM content_review_request" + where + " ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",
                this::map, args);
    }

    @Transactional
    public ContentReviewView submitBlogUpdate(Long actorId, Long postId, UpdatePostRequest request) {
        Integer pending = jdbc.queryForObject("""
                SELECT COUNT(*) FROM content_review_request
                WHERE content_type='BLOG_POST' AND content_id=? AND status='PENDING'
                """, Integer.class, postId);
        if (pending != null && pending > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "CONTENT_REVIEW_PENDING",
                    "Review already pending", "这篇文章已有待审核版本，请等待超级管理员处理。");
        }
        jdbc.update("""
                INSERT INTO content_review_request(content_type,content_id,action_type,title,payload_json,submitted_by)
                VALUES ('BLOG_POST',?,'UPDATE',?,?,?)
                """, postId, request.title(), json.valueToTree(request).toString(), actorId);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        auditLogService.record(actorId, "CONTENT_REVIEW_SUBMITTED", "BLOG_POST", postId, "SUCCESS", null, null,
                Map.of("reviewId", id, "action", "UPDATE"));
        return get(id);
    }

    public boolean isPublished(String contentType, Long contentId) {
        String table = switch (contentType) {
            case "ENGLISH_GRAMMAR_LESSON" -> "english_grammar_lesson";
            case "ENGLISH_READING_ARTICLE" -> "english_reading_article";
            case "ENGLISH_LISTENING_ITEM" -> "english_listening_item";
            case "ENGLISH_PRONUNCIATION_RULE" -> "english_listening_pronunciation_rule";
            case "ENGLISH_WRITING_RESOURCE" -> "english_writing_resource";
            case "ENGLISH_WRITING_PROMPT" -> "english_writing_prompt";
            default -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTENT_REVIEW_TYPE_INVALID",
                    "Invalid review type", "Unsupported review content type.");
        };
        try {
            String status = jdbc.queryForObject("SELECT publish_status FROM " + table + " WHERE id=?", String.class, contentId);
            return "PUBLISHED".equals(status);
        } catch (EmptyResultDataAccessException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CONTENT_REVIEW_TARGET_NOT_FOUND",
                    "Review target not found", "The content to review does not exist.");
        }
    }

    @Transactional
    public ContentReviewView submitEnglishUpdate(Long actorId, String contentType, Long contentId,
                                                 String title, Object request) {
        Integer pending = jdbc.queryForObject("""
                SELECT COUNT(*) FROM content_review_request
                WHERE content_type=? AND content_id=? AND status='PENDING'
                """, Integer.class, contentType, contentId);
        if (pending != null && pending > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "CONTENT_REVIEW_PENDING",
                    "Review already pending", "这条内容已有待审核版本，请等待超级管理员处理。");
        }
        jdbc.update("""
                INSERT INTO content_review_request(content_type,content_id,action_type,title,payload_json,submitted_by)
                VALUES (?,?,'UPDATE',?,?,?)
                """, contentType, contentId, title, json.valueToTree(request).toString(), actorId);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        auditLogService.record(actorId, "CONTENT_REVIEW_SUBMITTED", contentType, contentId, "SUCCESS", null, null,
                Map.of("reviewId", id, "action", "UPDATE"));
        return get(id);
    }

    @Transactional
    public ContentReviewView submitTutorialChapterUpdate(Long actorId, Long tutorialId, Long chapterId,
                                                         UpdateChapterRequest request) {
        Integer pending = jdbc.queryForObject("""
                SELECT COUNT(*) FROM content_review_request
                WHERE content_type='TUTORIAL_CHAPTER' AND content_id=? AND status='PENDING'
                """, Integer.class, chapterId);
        if (pending != null && pending > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "CONTENT_REVIEW_PENDING",
                    "Review already pending", "这个章节已有待审核版本，请等待超级管理员处理。");
        }
        jdbc.update("""
                INSERT INTO content_review_request(content_type,content_id,action_type,title,payload_json,submitted_by)
                VALUES ('TUTORIAL_CHAPTER',?,'UPDATE',?,?,?)
                """, chapterId, request.title(), json.valueToTree(Map.of(
                        "tutorialId", tutorialId,
                        "request", request
                )).toString(), actorId);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        auditLogService.record(actorId, "CONTENT_REVIEW_SUBMITTED", "TUTORIAL_CHAPTER", chapterId, "SUCCESS", null, null,
                Map.of("reviewId", id, "action", "UPDATE"));
        return get(id);
    }

    @Transactional
    public ContentReviewView approve(Long id, Long reviewerId, String note) {
        ContentReviewView review = get(id);
        ensurePending(review);
        if ("BLOG_POST".equals(review.contentType()) && "UPDATE".equals(review.actionType())) {
            UpdatePostRequest request = json.convertValue(review.payload(), UpdatePostRequest.class);
            blogService.update(review.contentId(), request);
        } else if ("TUTORIAL_CHAPTER".equals(review.contentType()) && "UPDATE".equals(review.actionType())) {
            Long tutorialId = json.convertValue(review.payload().get("tutorialId"), Long.class);
            UpdateChapterRequest request = json.convertValue(review.payload().get("request"), UpdateChapterRequest.class);
            tutorialNodeService.updateChapter(tutorialId, review.contentId(), request);
        } else if ("ENGLISH_GRAMMAR_LESSON".equals(review.contentType()) && "UPDATE".equals(review.actionType())) {
            grammarService.updateLesson(review.contentId(), json.convertValue(review.payload(), GrammarLessonRequest.class));
        } else if ("ENGLISH_READING_ARTICLE".equals(review.contentType()) && "UPDATE".equals(review.actionType())) {
            readingService.update(review.contentId(), json.convertValue(review.payload(), ReadingArticleRequest.class));
        } else if ("ENGLISH_LISTENING_ITEM".equals(review.contentType()) && "UPDATE".equals(review.actionType())) {
            listeningService.update(review.contentId(), json.convertValue(review.payload(), ListeningItemRequest.class));
        } else if ("ENGLISH_PRONUNCIATION_RULE".equals(review.contentType()) && "UPDATE".equals(review.actionType())) {
            listeningService.updateRule(review.contentId(), json.convertValue(review.payload(), PronunciationRuleRequest.class));
        } else if ("ENGLISH_WRITING_RESOURCE".equals(review.contentType()) && "UPDATE".equals(review.actionType())) {
            writingResourceService.update(review.contentId(), json.convertValue(review.payload(), WritingResourceRequest.class));
        } else if ("ENGLISH_WRITING_PROMPT".equals(review.contentType()) && "UPDATE".equals(review.actionType())) {
            writingPromptService.update(review.contentId(), json.convertValue(review.payload(), WritingPromptRequest.class));
        }
        jdbc.update("""
                UPDATE content_review_request
                SET status='APPROVED',reviewed_by=?,review_note=?,reviewed_at=UTC_TIMESTAMP(6)
                WHERE id=? AND status='PENDING'
                """, reviewerId, clean(note), id);
        auditLogService.record(reviewerId, "CONTENT_REVIEW_APPROVED", review.contentType(), review.contentId(), "SUCCESS", null, null,
                Map.of("reviewId", id, "action", review.actionType()));
        return get(id);
    }

    @Transactional
    public ContentReviewView reject(Long id, Long reviewerId, String note) {
        ContentReviewView review = get(id);
        ensurePending(review);
        jdbc.update("""
                UPDATE content_review_request
                SET status='REJECTED',reviewed_by=?,review_note=?,reviewed_at=UTC_TIMESTAMP(6)
                WHERE id=? AND status='PENDING'
                """, reviewerId, clean(note), id);
        auditLogService.record(reviewerId, "CONTENT_REVIEW_REJECTED", review.contentType(), review.contentId(), "SUCCESS", null, null,
                Map.of("reviewId", id, "action", review.actionType()));
        return get(id);
    }

    private ContentReviewView get(Long id) {
        try {
            return jdbc.queryForObject("SELECT * FROM content_review_request WHERE id=?", this::map, id);
        } catch (EmptyResultDataAccessException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CONTENT_REVIEW_NOT_FOUND",
                    "Review not found", "The requested review does not exist.");
        }
    }

    private ContentReviewView map(ResultSet rs, int row) throws SQLException {
        return new ContentReviewView(rs.getLong("id"), rs.getString("content_type"), rs.getLong("content_id"),
                rs.getString("action_type"), rs.getString("title"),
                readPayload(rs.getString("payload_json")),
                rs.getString("status"), nullableLong(rs, "submitted_by"), nullableLong(rs, "reviewed_by"),
                rs.getString("review_note"), format(rs, "created_at"), format(rs, "updated_at"), format(rs, "reviewed_at"));
    }

    private void ensurePending(ContentReviewView review) {
        if (!"PENDING".equals(review.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "CONTENT_REVIEW_CLOSED",
                    "Review already closed", "该审核已处理，不能重复操作。");
        }
    }

    private Map<String, Object> readPayload(String value) {
        try {
            return json.readValue(value, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Invalid review payload JSON", e);
        }
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String format(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timezone.atSite(timestamp.toLocalDateTime()).format(ISO);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
