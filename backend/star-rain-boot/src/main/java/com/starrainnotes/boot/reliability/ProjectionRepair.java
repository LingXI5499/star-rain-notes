package com.starrainnotes.boot.reliability;

import com.starrainnotes.search.service.SearchRebuildService;
import com.starrainnotes.seo.service.SeoRebuildService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Coalesced transactional outbox: a committed publication change always leaves a durable repair watermark. */
@Component
public class ProjectionRepair {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectionRepair.class);
    private final JdbcTemplate jdbc;
    private final SearchRebuildService search;
    private final SeoRebuildService seo;
    private final org.springframework.transaction.support.TransactionTemplate acknowledgements;
    private final java.util.Map<Object, java.util.Map<String, Long>> pending = new java.util.IdentityHashMap<>();

    public ProjectionRepair(JdbcTemplate jdbc, SearchRebuildService search, SeoRebuildService seo) {
        this.jdbc = jdbc; this.search = search; this.seo = seo;
        this.acknowledgements = new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(java.util.Objects.requireNonNull(jdbc.getDataSource())));
        this.acknowledgements.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void changed(Object event) {
        if (event instanceof org.springframework.context.PayloadApplicationEvent<?> payload) event = payload.getPayload();
        String name = event.getClass().getName();
        if (List.of("BlogPostPublishedEvent", "BlogPostWithdrawnEvent", "BlogPostChangedEvent",
                "BlogTopicChangedEvent", "WorkPublicationChangedEvent", "ProfileChangedEvent",
                "TutorialPublicationChangedEvent", "SiteConfigChangedEvent", "EnglishSearchContentChangedEvent")
                .stream().noneMatch(name::endsWith)) return;
        var projections = name.endsWith("EnglishSearchContentChangedEvent") ? List.of("SEARCH")
                : name.endsWith("SiteConfigChangedEvent") || name.endsWith("BlogTopicChangedEvent") ? List.of("SEO") : List.of("SEARCH", "SEO");
        var revisions = new java.util.HashMap<String, Long>();
        for (String projection : projections) {
            jdbc.update("UPDATE sr_projection_repair SET requested_version=requested_version+1,next_attempt_at=UTC_TIMESTAMP(3) WHERE projection_name=?", projection);
            revisions.put(projection, jdbc.queryForObject("SELECT requested_version FROM sr_projection_repair WHERE projection_name=?", Long.class, projection));
        }
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            Object original = event;
            synchronized (pending) { pending.put(original, revisions); }
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCompletion(int status) { synchronized (pending) { pending.remove(original); } }
            });
        }
    }

    @org.springframework.context.event.EventListener
    public void handled(com.starrainnotes.common.event.ProjectionHandledEvent event) {
        Long version;
        synchronized (pending) {
            var revisions = pending.get(event.source());
            version = revisions == null ? null : revisions.remove(event.projection());
        }
        if (version == null || !event.successful()) return;
        // Never acknowledge an older failed generation, or a concurrent rebuild that may write an older snapshot.
        // AFTER_COMMIT still binds the completed transaction's connection. Commit acknowledgements independently.
        acknowledgements.executeWithoutResult(status -> jdbc.update(
                "UPDATE sr_projection_repair SET completed_version=? WHERE projection_name=? AND completed_version=? AND lease_token IS NULL",
                version, event.projection(), version - 1));
    }

    @Scheduled(fixedDelayString = "${star-rain.projections.reconcile-ms:900000}", initialDelay = 900000)
    public void requestRepair() {
        jdbc.update("UPDATE sr_projection_repair SET requested_version=requested_version+1, next_attempt_at=UTC_TIMESTAMP(3)");
    }

    @Scheduled(fixedDelayString = "${star-rain.projections.retry-ms:30000}", initialDelay = 30000)
    public void process() {
        var jobs = jdbc.query("SELECT projection_name,requested_version FROM sr_projection_repair "
                + "WHERE requested_version>completed_version AND (next_attempt_at<=UTC_TIMESTAMP(3) OR (completed_version=0 AND attempts=0)) "
                + "AND (lease_until IS NULL OR lease_until<UTC_TIMESTAMP(3))",
                (row, index) -> new Job(row.getString(1), row.getLong(2)));
        for (Job job : jobs) {
            String lease = UUID.randomUUID().toString();
            int claimed = jdbc.update("UPDATE sr_projection_repair SET lease_token=?,lease_until=DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 10 MINUTE) "
                    + "WHERE projection_name=? AND requested_version=? AND requested_version>completed_version AND (lease_until IS NULL OR lease_until<UTC_TIMESTAMP(3))",
                    lease, job.name(), job.version());
            if (claimed != 1) continue;
            try {
                if ("SEARCH".equals(job.name())) search.rebuildAll(); else seo.rebuildAll();
                jdbc.update("UPDATE sr_projection_repair SET completed_version=GREATEST(completed_version,?), "
                        + "lease_token=NULL,lease_until=NULL,attempts=0,last_error=NULL WHERE projection_name=? AND lease_token=?",
                        job.version(), job.name(), lease);
            } catch (RuntimeException failure) {
                jdbc.update("UPDATE sr_projection_repair SET attempts=attempts+1,last_error=?,lease_token=NULL,lease_until=NULL, "
                        + "next_attempt_at=DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 1 MINUTE) WHERE projection_name=? AND lease_token=?",
                        failure.getClass().getSimpleName(), job.name(), lease);
                LOG.error("Projection repair failed for {}", job.name(), failure);
            }
        }
    }

    private record Job(String name, long version) { }
}
