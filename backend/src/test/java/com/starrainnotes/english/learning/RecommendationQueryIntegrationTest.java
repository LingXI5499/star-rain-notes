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
    private static final String LISTENING_SLUG = "recommendation-fixture-listening";
    private static final String BUNDLE_SLUG = "recommendation-fixture-bundle";

    @Autowired JdbcTemplate jdbc;
    @Autowired RecommendationQueryService recommendations;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM english_learning_bundle WHERE slug=?", BUNDLE_SLUG);
        jdbc.update("DELETE FROM english_learner_profile WHERE learner_key_hash=?", LEARNER_HASH);
        jdbc.update("DELETE FROM english_reading_article WHERE slug=?", SLUG);
        jdbc.update("DELETE FROM english_listening_item WHERE slug=?", LISTENING_SLUG);
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

    @Test
    void recommendsNextPublishedBundleMemberAfterCompletedItem() {
        cleanup();
        jdbc.update("INSERT INTO english_learner_profile(learner_key_hash) VALUES (?)", LEARNER_HASH);
        long learnerId = jdbc.queryForObject(
                "SELECT id FROM english_learner_profile WHERE learner_key_hash=?", Long.class, LEARNER_HASH);
        jdbc.update("""
                INSERT INTO english_reading_article(title,slug,summary,body_markdown,reading_level,cefr_level,
                publish_status,sort_order,published_at)
                VALUES ('Recommendation reading',?,'Summary','Body',1,'B1','PUBLISHED',10,UTC_TIMESTAMP(6))
                """, SLUG);
        long readingId = jdbc.queryForObject(
                "SELECT id FROM english_reading_article WHERE slug=?", Long.class, SLUG);
        jdbc.update("""
                INSERT INTO english_listening_item(title,slug,summary,transcript_markdown,cefr_level,
                listening_level,duration_seconds,publish_status,sort_order,published_at)
                VALUES ('Recommendation listening',?,'Summary','Transcript','B1',1,60,'PUBLISHED',10,UTC_TIMESTAMP(6))
                """, LISTENING_SLUG);
        long listeningId = jdbc.queryForObject(
                "SELECT id FROM english_listening_item WHERE slug=?", Long.class, LISTENING_SLUG);
        jdbc.update("""
                INSERT INTO english_learning_bundle(title,slug,summary,primary_cefr,publish_status,published_at)
                VALUES ('Recommendation bundle',?,'Summary','B1','PUBLISHED',UTC_TIMESTAMP(6))
                """, BUNDLE_SLUG);
        long bundleId = jdbc.queryForObject(
                "SELECT id FROM english_learning_bundle WHERE slug=?", Long.class, BUNDLE_SLUG);
        jdbc.update("INSERT INTO english_learning_bundle_reading_item(bundle_id,article_id,sort_order) VALUES (?,?,10)",
                bundleId, readingId);
        jdbc.update("INSERT INTO english_learning_bundle_listening_item(bundle_id,listening_item_id,sort_order) VALUES (?,?,20)",
                bundleId, listeningId);
        jdbc.update("""
                INSERT INTO english_learning_record(learner_id,content_type,content_id,content_slug,
                completion_status,weak_points_json)
                VALUES (?,'READING',?,?,'COMPLETED','[]')
                """, learnerId, readingId, SLUG);

        List<LearningRecommendationView> result = recommendations.recommendations(learnerId);
        assertThat(result).anySatisfy(item -> {
            assertThat(item.recommendationType()).isEqualTo("BUNDLE_NEXT");
            assertThat(item.contentType()).isEqualTo("LISTENING");
            assertThat(item.contentId()).isEqualTo(listeningId);
            assertThat(item.sourceTitle()).isEqualTo("Recommendation bundle");
        });
    }

    @Test
    void recommendsPublishedTagMatchWithoutRepeatingStudiedContent() {
        cleanup();
        jdbc.update("INSERT INTO english_learner_profile(learner_key_hash) VALUES (?)", LEARNER_HASH);
        long learnerId = jdbc.queryForObject(
                "SELECT id FROM english_learner_profile WHERE learner_key_hash=?", Long.class, LEARNER_HASH);
        jdbc.update("""
                INSERT INTO english_reading_article(title,slug,summary,body_markdown,reading_level,cefr_level,
                publish_status,sort_order,published_at)
                VALUES ('Tag reading',?,'Summary','Body',1,'B1','PUBLISHED',10,UTC_TIMESTAMP(6))
                """, SLUG);
        long readingId = jdbc.queryForObject(
                "SELECT id FROM english_reading_article WHERE slug=?", Long.class, SLUG);
        jdbc.update("""
                INSERT INTO english_listening_item(title,slug,summary,transcript_markdown,cefr_level,
                listening_level,duration_seconds,publish_status,sort_order,published_at)
                VALUES ('Tag listening',?,'Summary','Transcript','B1',1,60,'PUBLISHED',10,UTC_TIMESTAMP(6))
                """, LISTENING_SLUG);
        long listeningId = jdbc.queryForObject(
                "SELECT id FROM english_listening_item WHERE slug=?", Long.class, LISTENING_SLUG);
        jdbc.update("INSERT INTO english_reading_article_tag(article_id,term_id,tag_role) VALUES (?,1,'TAG')",
                readingId);
        jdbc.update("INSERT INTO english_listening_item_tag(listening_item_id,term_id,tag_role) VALUES (?,1,'TAG')",
                listeningId);
        jdbc.update("""
                INSERT INTO english_learning_record(learner_id,content_type,content_id,content_slug,
                completion_status,weak_points_json)
                VALUES (?,'READING',?,?,'COMPLETED','[]')
                """, learnerId, readingId, SLUG);

        List<LearningRecommendationView> result = recommendations.recommendations(learnerId);
        assertThat(result).anySatisfy(item -> {
            assertThat(item.recommendationType()).isEqualTo("TAG_MATCH");
            assertThat(item.contentType()).isEqualTo("LISTENING");
            assertThat(item.contentId()).isEqualTo(listeningId);
        });
        assertThat(result).noneMatch(item -> item.contentType().equals("READING")
                && item.contentId() == readingId);

        jdbc.update("""
                INSERT INTO english_reading_listening_pair(reading_article_id,listening_item_id,relation_type,sort_order)
                VALUES (?,?,'SAME_TOPIC',10)
                """, readingId, listeningId);
        List<LearningRecommendationView> paired = recommendations.recommendations(learnerId);
        assertThat(paired).anySatisfy(item -> {
            assertThat(item.recommendationType()).isEqualTo("PAIRED");
            assertThat(item.contentType()).isEqualTo("LISTENING");
            assertThat(item.contentId()).isEqualTo(listeningId);
            assertThat(item.sourceTitle()).isEqualTo("Tag reading");
        });
    }
}
