package com.starrainnotes.english.shared.content;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.api.EnglishContentFacade;
import com.starrainnotes.english.api.EnglishReviewContentPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnglishContentFacadeIntegrationTest extends AbstractAuthIntegrationTest {
    private static final String SLUG = "content-facade-fixture";

    @Autowired EnglishContentFacade content;
    @Autowired EnglishReviewContentPort reviewContent;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM english_reading_article WHERE slug=?", SLUG);
    }

    @Test
    void exposesPublishedReadingReadinessAndTaxonomy() {
        cleanup();
        jdbc.update("""
                INSERT INTO english_reading_article(title,slug,summary,body_markdown,reading_level,cefr_level,
                publish_status,sort_order,published_at)
                VALUES ('Content facade',?,'Summary','Body',1,'B1','PUBLISHED',10,UTC_TIMESTAMP(6))
                """, SLUG);
        long id = jdbc.queryForObject(
                "SELECT id FROM english_reading_article WHERE slug=?", Long.class, SLUG);
        jdbc.update("INSERT INTO english_reading_article_tag(article_id,term_id,tag_role) VALUES (?,1,'TAG')", id);

        assertThat(content.requirePublished(EnglishContentType.READING, id).slug()).isEqualTo(SLUG);
        assertThat(content.listPublished(EnglishContentType.READING))
                .anyMatch(item -> item.id() == id);
        assertThat(content.readiness(EnglishContentType.READING, id).ready()).isTrue();
        assertThat(reviewContent.isPublished("ENGLISH_READING_ARTICLE", id)).isTrue();
        assertThat(content.taxonomy(EnglishContentType.READING, id))
                .anyMatch(term -> term.id() == 1 && term.dimension().equals("TOPIC"));

        jdbc.update("UPDATE english_reading_article SET publish_status='WITHDRAWN' WHERE id=?", id);
        assertThat(content.readiness(EnglishContentType.READING, id).issues())
                .containsExactly("CONTENT_UNPUBLISHED");
        assertThat(reviewContent.isPublished("ENGLISH_READING_ARTICLE", id)).isFalse();
        assertThatThrownBy(() -> content.requirePublished(EnglishContentType.READING, id))
                .isInstanceOf(ApiException.class);

        assertThatThrownBy(() -> reviewContent.isPublished("UNKNOWN", id))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getCode())
                .isEqualTo("CONTENT_REVIEW_TYPE_INVALID");
        jdbc.update("DELETE FROM english_reading_article_tag WHERE article_id=?", id);
        jdbc.update("DELETE FROM english_reading_article WHERE id=?", id);
        assertThatThrownBy(() -> reviewContent.isPublished("ENGLISH_READING_ARTICLE", id))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getCode())
                .isEqualTo("CONTENT_REVIEW_TARGET_NOT_FOUND");
    }
}
