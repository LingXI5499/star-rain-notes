package com.starrainnotes.seo;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SeoContentChangeAspect {
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;
    private final SeoProperties properties;

    public SeoContentChangeAspect(JdbcTemplate jdbc, ApplicationEventPublisher events, SeoProperties properties) {
        this.jdbc = jdbc;
        this.events = events;
        this.properties = properties;
    }

    @Around("@annotation(change)")
    public Object publishAfterChange(ProceedingJoinPoint invocation, SeoContentChange change) throws Throwable {
        Long id = firstLong(invocation.getArgs());
        ContentState before = id == null ? null : state(change.table(), id);
        Object result = invocation.proceed();
        ContentState current = id == null ? null : state(change.table(), id);
        ContentState visible = current != null && current.published() ? current
                : before != null && before.published() ? before : null;
        if (visible != null) {
            events.publishEvent(new SeoContentChangedEvent(properties.siteOrigin() + change.pathPrefix() + visible.slug()));
        }
        return result;
    }

    private ContentState state(String table, Long id) {
        if (!table.matches("[a-z_]+")) throw new IllegalArgumentException("Unsafe SEO table");
        return jdbc.query("SELECT slug,publish_status FROM " + table + " WHERE id=?",
                rs -> rs.next() ? new ContentState(rs.getString(1), "PUBLISHED".equals(rs.getString(2))) : null, id);
    }
    private Long firstLong(Object[] args) { for (Object arg : args) if (arg instanceof Long value) return value; return null; }
    private record ContentState(String slug, boolean published) {}
}
