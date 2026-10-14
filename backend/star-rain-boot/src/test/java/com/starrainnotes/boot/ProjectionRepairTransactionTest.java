package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.starrainnotes.blog.api.event.BlogPostPublishedEvent;
import com.starrainnotes.boot.reliability.ProjectionRepair;
import com.starrainnotes.common.event.ProjectionHandledEvent;
import com.starrainnotes.search.service.SearchRebuildService;
import com.starrainnotes.seo.service.SeoRebuildService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.TransactionTemplate;

/** Actual Spring transaction phases and actual MySQL watermarks, with repair work stubbed. */
class ProjectionRepairTransactionTest extends MapperXmlIntegrationSupport {
    @Test void acknowledgementsNeverSkipAFailedGenerationAndRepairRetriesDurably() {
        String url = System.getenv("STAR_RAIN_DB_URL");
        assertTrue(url != null && url.contains("127.0.0.1") && (url.contains("star_rain_ci") || url.contains("star_rain_v22")), "Use the isolated test database only");
        try (var session = openSession(); var context = new AnnotationConfigApplicationContext()) {
            var source = session.getConfiguration().getEnvironment().getDataSource();
            var jdbc = new JdbcTemplate(source); var transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
            var search = mock(SearchRebuildService.class); var seo = mock(SeoRebuildService.class);
            context.registerBean(JdbcTemplate.class, () -> jdbc);
            context.registerBean(SearchRebuildService.class, () -> search);
            context.registerBean(SeoRebuildService.class, () -> seo);
            context.registerBean(TransactionalEventListenerFactory.class);
            context.registerBean(ProjectionRepair.class);
            context.registerBean(FastAcks.class);
            context.refresh();
            jdbc.update("UPDATE sr_projection_repair SET requested_version=1,completed_version=1,lease_token=NULL,lease_until=NULL,next_attempt_at=UTC_TIMESTAMP(3),attempts=0");
            try {
                transaction.executeWithoutResult(status -> { context.publishEvent(event()); status.setRollbackOnly(); });
                assertEquals(2L, sum(jdbc, "requested_version"), "a rolled-back change creates no repair watermark");
                var fast = context.getBean(FastAcks.class); fast.searchSucceeds = false;
                transaction.executeWithoutResult(status -> context.publishEvent(event()));
                assertEquals(1L, value(jdbc, "SEARCH", "completed_version"));
                assertEquals(2L, value(jdbc, "SEO", "completed_version"));
                fast.searchSucceeds = true;
                transaction.executeWithoutResult(status -> context.publishEvent(event()));
                assertEquals(1L, value(jdbc, "SEARCH", "completed_version"), "a later successful event cannot hide the older failure");
                assertEquals(3L, value(jdbc, "SEO", "completed_version"));
                doThrow(new IllegalStateException("injected projection failure")).doNothing().when(search).rebuildAll();
                var repair = context.getBean(ProjectionRepair.class); repair.process();
                assertEquals(1L, value(jdbc, "SEARCH", "attempts"));
                assertEquals(1L, value(jdbc, "SEARCH", "completed_version"));
                jdbc.update("UPDATE sr_projection_repair SET next_attempt_at=UTC_TIMESTAMP(3) WHERE projection_name='SEARCH'");
                repair.process();
                assertEquals(3L, value(jdbc, "SEARCH", "completed_version"));
                transaction.executeWithoutResult(status -> context.publishEvent(event()));
                assertEquals(4L, value(jdbc, "SEARCH", "completed_version"));
                repair.process(); verify(search, times(2)).rebuildAll(); verifyNoInteractions(seo);
            } finally {
                jdbc.update("UPDATE sr_projection_repair SET requested_version=1,completed_version=0,lease_token=NULL,lease_until=NULL,next_attempt_at=UTC_TIMESTAMP(3),attempts=0,last_error=NULL");
            }
        }
    }
    private static BlogPostPublishedEvent event() { return BlogPostPublishedEvent.builder().postId(1L).slug("test").title("test").build(); }
    private static long sum(JdbcTemplate jdbc, String column) { return jdbc.queryForObject("SELECT SUM(" + column + ") FROM sr_projection_repair", Long.class); }
    private static long value(JdbcTemplate jdbc, String name, String column) { return jdbc.queryForObject("SELECT " + column + " FROM sr_projection_repair WHERE projection_name=?", Long.class, name); }
    static class FastAcks {
        private final ProjectionRepair repair; boolean searchSucceeds = true;
        FastAcks(ProjectionRepair repair) { this.repair = repair; }
        @TransactionalEventListener public void handled(BlogPostPublishedEvent event) {
            repair.handled(new ProjectionHandledEvent(event, "SEARCH", searchSucceeds));
            repair.handled(new ProjectionHandledEvent(event, "SEO", true));
        }
    }
}
