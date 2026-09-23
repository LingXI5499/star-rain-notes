package com.starrainnotes.english.learning;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import com.starrainnotes.english.learning.application.RecommendationQueryService;
import com.starrainnotes.english.learning.dto.LearningRecommendationView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationQueryIntegrationTest extends AbstractAuthIntegrationTest {
    private static final String LEARNER_HASH = "r".repeat(64);
    private static final String SLUG = "recommendation-fixture-reading";

    @Autowired JdbcTemplate jdbc;
    @Autowired RecommendationQueryService recommendations;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM english_learner_profile WHERE learner_key_hash=?", LEARNER_HASH);
        jdbc.update("DELETE FROM english_reading_article WHERE slug=?", SLUG);
    }

    @Test
    void dueReviewPrecedesStartersAndDoesNotRepeatTheSameContent() {
        cleanup();
        jdbc.update("INSERT INTO english_learner_profile(learner_key_hash) VALUES (?)", LEARNER_HASH);
        long learnerId = jdbc.queryForObject(
                "SELECT id FROM english_learner_profile WHERE learner_key_hash=?", Long.class, LEARNER_HASH);
        jdbc.update("""
                INSERT INTO english_reading_article(title,slug,summary,body_markdown,reading_level,cefr_level,
                publish_status,sort_order,published_at)
                VALUES ('Recommendation fixture',?,'Summary','Body',1,'B1','PUBLISHED',10,UTC_TIMESTAMP(6))
                """, SLUG);
        long readingId = jdbc.queryForObject(
                "SELECT id FROM english_reading_article WHERE slug=?", Long.class, SLUG);
        jdbc.update("""
                INSERT INTO english_learning_record(learner_id,content_type,content_id,content_slug,
                completion_status,weak_points_json,mastery_level,next_review_at)
                VALUES (?,'READING',?,?,'IN_PROGRESS','[]',0.4000,UTC_TIMESTAMP(6)-INTERVAL 1 DAY)
                """, learnerId, readingId, SLUG);

        List<LearningRecommendationView> result = recommendations.recommendations(learnerId);
        assertThat(result).isNotEmpty().hasSizeLessThanOrEqualTo(8);
        assertThat(result.get(0).contentType()).isEqualTo("READING");
        assertThat(result.get(0).contentId()).isEqualTo(readingId);
        assertThat(result.get(0).recommendationType()).isEqualTo("REVIEW");
        assertThat(result.get(0).priority()).isEqualTo(10);
        assertThat(result.get(0).route()).isEqualTo("/english/reading/" + SLUG);
        assertThat(result.get(0).nextReviewAt()).isNotNull();
        assertThat(result.stream().filter(item -> item.contentType().equals("READING")
                && item.contentId() == readingId)).hasSize(1);

        jdbc.update("UPDATE english_reading_article SET publish_status='WITHDRAWN' WHERE id=?", readingId);
        assertThat(recommendations.recommendations(learnerId)).noneMatch(
                item -> item.contentType().equals("READING") && item.contentId() == readingId);
    }
}
