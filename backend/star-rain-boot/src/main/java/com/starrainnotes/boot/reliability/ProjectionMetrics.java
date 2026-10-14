package com.starrainnotes.boot.reliability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProjectionMetrics implements MeterBinder {
    private final JdbcTemplate jdbc;
    public ProjectionMetrics(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public void bindTo(MeterRegistry registry) {
        io.micrometer.core.instrument.Gauge.builder("star.rain.projection.pending", jdbc,
                source -> source.queryForObject("SELECT COUNT(*) FROM sr_projection_repair WHERE requested_version>completed_version", Double.class)).register(registry);
        io.micrometer.core.instrument.Gauge.builder("star.rain.mail.pending", jdbc,
                source -> source.queryForObject("SELECT COUNT(*) FROM sr_mail_outbox WHERE expires_at>UTC_TIMESTAMP(3)", Double.class)).register(registry);
    }
}
