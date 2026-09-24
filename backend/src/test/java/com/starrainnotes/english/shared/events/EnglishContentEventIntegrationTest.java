package com.starrainnotes.english.shared.events;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.reading.application.ReadingCommandService;
import com.starrainnotes.seo.SeoContentChangedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RecordApplicationEvents
class EnglishContentEventIntegrationTest extends AbstractAuthIntegrationTest {
    private static final String SLUG = "english-event-fixture";

    @Autowired JdbcTemplate jdbc;
    @Autowired ReadingCommandService reading;
    @Autowired ApplicationEvents events;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM english_reading_article WHERE slug=?", SLUG);
    }

    @Test
    void publishedWithdrawalEmitsDomainEventAndSeoNotification() {
        cleanup();
        long id = insertReading("PUBLISHED");

        reading.withdraw(id);

        assertThat(events.stream(EnglishContentChangedEvent.class))
                .anyMatch(event -> event.contentType() == EnglishContentKind.READING
                        && event.contentId() == id
                        && event.slug().equals(SLUG)
                        && event.changeType() == EnglishContentChangeType.WITHDRAWN);
        assertThat(events.stream(SeoContentChangedEvent.class)
                .filter(event -> event.absoluteUrl().endsWith("/english/reading/" + SLUG)))
                .hasSize(1);
    }

    @Test
    void rejectedDraftWithdrawalEmitsNoEvent() {
        cleanup();
        long id = insertReading("DRAFT");

        assertThatThrownBy(() -> reading.withdraw(id)).isInstanceOf(ApiException.class);
        assertThat(events.stream(EnglishContentChangedEvent.class)).isEmpty();
    }

    private long insertReading(String status) {
        jdbc.update("""
                INSERT INTO english_reading_article(title,slug,summary,body_markdown,reading_level,cefr_level,
                publish_status,sort_order,published_at)
                VALUES ('Event fixture',?,'Summary','Body',1,'B1',?,10,
                        CASE WHEN ?='PUBLISHED' THEN UTC_TIMESTAMP(6) ELSE NULL END)
                """, SLUG, status, status);
        return jdbc.queryForObject(
                "SELECT id FROM english_reading_article WHERE slug=?", Long.class, SLUG);
    }
}
