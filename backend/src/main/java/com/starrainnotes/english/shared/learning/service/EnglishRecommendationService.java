package com.starrainnotes.english.shared.learning.service;

import com.starrainnotes.english.shared.learning.dto.LearningRecommendationView;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnglishRecommendationService {
    private static final int LIMIT = 8;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String CONTENT_CATALOG = """
            SELECT 'GRAMMAR' content_type,l.id content_id,l.slug,l.title,NULL cefr_level
            FROM english_grammar_lesson l JOIN english_grammar_course c ON c.id=l.course_id
            WHERE l.publish_status='PUBLISHED' AND c.publish_status='PUBLISHED'
            UNION ALL
            SELECT 'READING',id,slug,title,cefr_level FROM english_reading_article WHERE publish_status='PUBLISHED'
            UNION ALL
            SELECT 'LISTENING',id,slug,title,cefr_level FROM english_listening_item WHERE publish_status='PUBLISHED'
            UNION ALL
            SELECT 'WRITING',id,slug,title,cefr_level FROM english_writing_prompt WHERE publish_status='PUBLISHED'
            """;

    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;

    public EnglishRecommendationService(JdbcTemplate jdbc, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.timezone = timezone;
    }

    public List<LearningRecommendationView> recommendations(long learnerId) {
        LinkedHashMap<Key, LearningRecommendationView> result = new LinkedHashMap<>();
        add(result, reviewAndContinue(learnerId));
        add(result, bundleNextSteps(learnerId));
        add(result, pairedContent(learnerId));
        add(result, tagMatches(learnerId));
        if (result.size() < 4) add(result, starters(learnerId));
        return result.values().stream()
                .sorted(Comparator.comparingInt(LearningRecommendationView::priority))
                .limit(LIMIT)
                .toList();
    }

    private List<LearningRecommendationView> reviewAndContinue(long learnerId) {
        return jdbc.query("""
                SELECT r.content_type,r.content_id,c.slug,c.title,c.cefr_level,r.mastery_level,r.next_review_at,
                       CASE WHEN r.next_review_at<=UTC_TIMESTAMP(6) THEN 'REVIEW' ELSE 'CONTINUE' END recommendation_type,
                       CASE WHEN r.next_review_at<=UTC_TIMESTAMP(6) THEN 10 ELSE 20 END priority
                FROM english_learning_record r
                JOIN (
                """ + CONTENT_CATALOG + """
                ) c
                  ON c.content_type=r.content_type AND c.content_id=r.content_id
                WHERE r.learner_id=?
                  AND (r.completion_status='IN_PROGRESS' OR r.next_review_at<=UTC_TIMESTAMP(6))
                ORDER BY priority,r.next_review_at,r.mastery_level,r.updated_at DESC
                LIMIT 8
                """, (rs, row) -> recommendation(rs,
                "REVIEW".equals(rs.getString("recommendation_type")) ? "到期复习" : "继续学习",
                rs.getString("recommendation_type"), rs.getInt("priority"), null), learnerId);
    }

    private List<LearningRecommendationView> bundleNextSteps(long learnerId) {
        List<BundleRow> rows = jdbc.query("""
                SELECT x.bundle_id,x.bundle_title,x.content_type,x.content_id,x.slug,x.title,x.cefr_level,x.sort_order,
                       r.completion_status
                FROM (
                  SELECT b.id bundle_id,b.title bundle_title,'READING' content_type,a.id content_id,a.slug,a.title,a.cefr_level,i.sort_order
                  FROM english_learning_bundle b
                  JOIN english_learning_bundle_reading_item i ON i.bundle_id=b.id
                  JOIN english_reading_article a ON a.id=i.article_id AND a.publish_status='PUBLISHED'
                  WHERE b.publish_status='PUBLISHED'
                  UNION ALL
                  SELECT b.id,b.title,'LISTENING',a.id,a.slug,a.title,a.cefr_level,i.sort_order
                  FROM english_learning_bundle b
                  JOIN english_learning_bundle_listening_item i ON i.bundle_id=b.id
                  JOIN english_listening_item a ON a.id=i.listening_item_id AND a.publish_status='PUBLISHED'
                  WHERE b.publish_status='PUBLISHED'
                  UNION ALL
                  SELECT b.id,b.title,'WRITING',a.id,a.slug,a.title,a.cefr_level,i.sort_order
                  FROM english_learning_bundle b
                  JOIN english_learning_bundle_writing_item i ON i.bundle_id=b.id
                  JOIN english_writing_prompt a ON a.id=i.prompt_id AND a.publish_status='PUBLISHED'
                  WHERE b.publish_status='PUBLISHED'
                ) x
                LEFT JOIN english_learning_record r ON r.learner_id=? AND r.content_type=x.content_type AND r.content_id=x.content_id
                ORDER BY x.bundle_id,x.sort_order,x.content_type,x.content_id
                """, (rs, row) -> new BundleRow(rs.getLong("bundle_id"), rs.getString("bundle_title"),
                candidate(rs), rs.getString("completion_status")), learnerId);
        Map<Long, List<BundleRow>> grouped = new LinkedHashMap<>();
        rows.forEach(row -> grouped.computeIfAbsent(row.bundleId(), ignored -> new ArrayList<>()).add(row));
        List<LearningRecommendationView> result = new ArrayList<>();
        for (List<BundleRow> path : grouped.values()) {
            boolean started = path.stream().anyMatch(row -> row.status() != null && !"NOT_STARTED".equals(row.status()));
            if (!started) continue;
            path.stream().filter(row -> !"COMPLETED".equals(row.status())).findFirst().ifPresent(row ->
                    result.add(view(row.candidate(), "学习路径下一步", "BUNDLE_NEXT", 30, row.bundleTitle())));
        }
        return result;
    }

    private List<LearningRecommendationView> pairedContent(long learnerId) {
        return jdbc.query("""
                SELECT target.* ,source.title source_title FROM (
                  SELECT 'LISTENING' content_type,l.id content_id,l.slug,l.title,l.cefr_level,
                         r.updated_at source_updated,p.reading_article_id source_id,'READING' source_type
                  FROM english_learning_record r
                  JOIN english_reading_listening_pair p ON p.reading_article_id=r.content_id
                  JOIN english_listening_item l ON l.id=p.listening_item_id AND l.publish_status='PUBLISHED'
                  WHERE r.learner_id=? AND r.content_type='READING' AND r.completion_status='COMPLETED'
                    AND NOT EXISTS(SELECT 1 FROM english_learning_record done WHERE done.learner_id=r.learner_id
                      AND done.content_type='LISTENING' AND done.content_id=l.id AND done.completion_status='COMPLETED')
                  UNION ALL
                  SELECT 'READING',a.id,a.slug,a.title,a.cefr_level,
                         r.updated_at,p.listening_item_id,'LISTENING'
                  FROM english_learning_record r
                  JOIN english_reading_listening_pair p ON p.listening_item_id=r.content_id
                  JOIN english_reading_article a ON a.id=p.reading_article_id AND a.publish_status='PUBLISHED'
                  WHERE r.learner_id=? AND r.content_type='LISTENING' AND r.completion_status='COMPLETED'
                    AND NOT EXISTS(SELECT 1 FROM english_learning_record done WHERE done.learner_id=r.learner_id
                      AND done.content_type='READING' AND done.content_id=a.id AND done.completion_status='COMPLETED')
                ) target
                JOIN (
                  SELECT 'READING' content_type,id,title FROM english_reading_article
                  UNION ALL SELECT 'LISTENING',id,title FROM english_listening_item
                ) source ON source.content_type=target.source_type AND source.id=target.source_id
                ORDER BY target.source_updated DESC LIMIT 6
                """, (rs, row) -> view(candidate(rs), "读听配对强化", "PAIRED", 40,
                rs.getString("source_title")), learnerId, learnerId);
    }

    private List<LearningRecommendationView> tagMatches(long learnerId) {
        List<Long> terms = jdbc.query("""
                SELECT tags.term_id FROM (
                  SELECT t.term_id,r.updated_at FROM english_learning_record r
                  JOIN english_reading_article_tag t ON r.content_type='READING' AND t.article_id=r.content_id
                  WHERE r.learner_id=? AND r.completion_status='COMPLETED'
                  UNION ALL
                  SELECT t.term_id,r.updated_at FROM english_learning_record r
                  JOIN english_listening_item_tag t ON r.content_type='LISTENING' AND t.listening_item_id=r.content_id
                  WHERE r.learner_id=? AND r.completion_status='COMPLETED'
                  UNION ALL
                  SELECT t.term_id,r.updated_at FROM english_learning_record r
                  JOIN english_writing_prompt_tag t ON r.content_type='WRITING' AND t.prompt_id=r.content_id
                  WHERE r.learner_id=? AND r.completion_status='COMPLETED'
                ) tags GROUP BY tags.term_id ORDER BY MAX(tags.updated_at) DESC LIMIT 20
                """, (rs, row) -> rs.getLong(1), learnerId, learnerId, learnerId);
        if (terms.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(terms.size(), "?"));
        List<Object> args = new ArrayList<>();
        args.add(learnerId);
        args.addAll(terms);
        args.add(learnerId);
        args.addAll(terms);
        args.add(learnerId);
        args.addAll(terms);
        return jdbc.query("""
                SELECT content_type,content_id,slug,title,cefr_level,COUNT(DISTINCT term_id) matches FROM (
                  SELECT 'READING' content_type,a.id content_id,a.slug,a.title,a.cefr_level,t.term_id
                  FROM english_reading_article a JOIN english_reading_article_tag t ON t.article_id=a.id
                  WHERE a.publish_status='PUBLISHED' AND NOT EXISTS(SELECT 1 FROM english_learning_record r
                    WHERE r.learner_id=? AND r.content_type='READING' AND r.content_id=a.id)
                    AND t.term_id IN (
                """ + placeholders + """
                    )
                  UNION ALL
                  SELECT 'LISTENING',a.id,a.slug,a.title,a.cefr_level,t.term_id
                  FROM english_listening_item a JOIN english_listening_item_tag t ON t.listening_item_id=a.id
                  WHERE a.publish_status='PUBLISHED' AND NOT EXISTS(SELECT 1 FROM english_learning_record r
                    WHERE r.learner_id=? AND r.content_type='LISTENING' AND r.content_id=a.id)
                    AND t.term_id IN (
                """ + placeholders + """
                    )
                  UNION ALL
                  SELECT 'WRITING',a.id,a.slug,a.title,a.cefr_level,t.term_id
                  FROM english_writing_prompt a JOIN english_writing_prompt_tag t ON t.prompt_id=a.id
                  WHERE a.publish_status='PUBLISHED' AND NOT EXISTS(SELECT 1 FROM english_learning_record r
                    WHERE r.learner_id=? AND r.content_type='WRITING' AND r.content_id=a.id)
                    AND t.term_id IN (
                """ + placeholders + """
                    )
                ) candidates GROUP BY content_type,content_id,slug,title,cefr_level
                ORDER BY matches DESC,content_type,content_id LIMIT 6
                """, (rs, row) -> view(candidate(rs), "同主题跨模块练习", "TAG_MATCH", 50, null), args.toArray());
    }

    private List<LearningRecommendationView> starters(long learnerId) {
        return jdbc.query("""
                SELECT c.* FROM (
                """ + CONTENT_CATALOG + """
                ) c
                WHERE NOT EXISTS(SELECT 1 FROM english_learning_record r WHERE r.learner_id=?
                  AND r.content_type=c.content_type AND r.content_id=c.content_id)
                ORDER BY FIELD(c.content_type,'GRAMMAR','READING','LISTENING','WRITING'),c.content_id
                """, (rs, row) -> view(candidate(rs), "建议开始", "STARTER", 60, null), learnerId)
                .stream().collect(java.util.stream.Collectors.toMap(
                        LearningRecommendationView::contentType, item -> item, (first, ignored) -> first,
                        LinkedHashMap::new)).values().stream().toList();
    }

    private LearningRecommendationView recommendation(ResultSet rs, String reason, String type,
                                                       int priority, String source) throws SQLException {
        Candidate candidate = candidate(rs);
        var timestamp = rs.getTimestamp("next_review_at");
        String nextReview = timestamp == null ? null : timezone.atSite(timestamp.toLocalDateTime()).format(ISO);
        return new LearningRecommendationView(candidate.type(), candidate.id(), candidate.slug(), candidate.title(),
                route(candidate.type(), candidate.slug()), reason, candidate.cefr(),
                rs.getBigDecimal("mastery_level"), nextReview, type, priority, source);
    }

    private LearningRecommendationView view(Candidate candidate, String reason, String type,
                                            int priority, String source) {
        return new LearningRecommendationView(candidate.type(), candidate.id(), candidate.slug(), candidate.title(),
                route(candidate.type(), candidate.slug()), reason, candidate.cefr(), null, null,
                type, priority, source);
    }

    private Candidate candidate(ResultSet rs) throws SQLException {
        return new Candidate(rs.getString("content_type"), rs.getLong("content_id"), rs.getString("slug"),
                rs.getString("title"), rs.getString("cefr_level"));
    }

    private void add(Map<Key, LearningRecommendationView> result, List<LearningRecommendationView> candidates) {
        for (LearningRecommendationView candidate : candidates) {
            result.putIfAbsent(new Key(candidate.contentType(), candidate.contentId()), candidate);
            if (result.size() >= LIMIT) return;
        }
    }

    private String route(String type, String slug) {
        return switch (type) {
            case "GRAMMAR" -> "/english/grammar/" + slug;
            case "READING" -> "/english/reading/" + slug;
            case "LISTENING" -> "/english/listening/" + slug;
            default -> "/english/writing/practice/" + slug;
        };
    }

    private record Key(String type, Long id) {}
    private record Candidate(String type, Long id, String slug, String title, String cefr) {}
    private record BundleRow(Long bundleId, String bundleTitle, Candidate candidate, String status) {}
}
