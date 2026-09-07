package com.starrainnotes.seo;

import com.starrainnotes.blog.dto.UpdatePostRequest;
import com.starrainnotes.blog.service.BlogService;
import com.starrainnotes.site.service.SiteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;

/** Opt-in repeatable lab probe. Uses only the test profile and rolls fixtures back. */
@SpringBootTest
@ActiveProfiles("test")
@Import(ExperiencePerformanceTest.ProbeConfig.class)
@EnabledIfSystemProperty(named = "experience.benchmark", matches = "true")
@Transactional
class ExperiencePerformanceTest {
    static final ThreadLocal<Integer> statements = ThreadLocal.withInitial(() -> 0);
    @Autowired JdbcTemplate jdbc;
    @Autowired BlogService blogs;
    @Autowired SiteService sites;
    @Autowired SeoContentRepository content;
    @Autowired SeoDocumentCache pages;
    @Autowired SeoHtmlRenderer renderer;

    @Test void measureFixedDataset() {
        String body = "## Performance fixture\n\nThis is synthetic test content.\n".repeat(1000);
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < 240; i++) rows.add(new Object[]{"Performance fixture " + i, "perf-fixture-" + i, body});
        jdbc.batchUpdate("INSERT INTO blog_post(title,slug,summary,body_markdown,publish_status,published_at) VALUES (?,?,'Benchmark fixture',?,'PUBLISHED','2026-09-01 00:00:00')", rows);
        Long id = jdbc.queryForObject("SELECT id FROM blog_post WHERE slug='perf-fixture-0'", Long.class);
        pages.contentChanged(new SeoContentChangedEvent("https://yulanlin.cn/blog"));
        measure("public-blog-list", () -> blogs.publicList(null, null, null, 1, 20));
        measure("blog-calendar", () -> blogs.calendar("2026-09"));
        measure("blog-archive", blogs::archive);
        measure("admin-blog-list-service", () -> blogs.adminList(1, 20, null, null, null));
        measure("admin-blog-save-service", () -> blogs.update(id, new UpdatePostRequest("Performance fixture 0", "perf-fixture-0", "Benchmark fixture", body, null, null, null, List.of(), List.of())));
        measure("seo-document", () -> renderer.render(pages.page("/blog/perf-fixture-0", () -> content.resolve("/blog/perf-fixture-0"))));
        var explain = jdbc.queryForList("EXPLAIN SELECT id,title,slug,summary,cover_media_id,published_at,updated_at FROM blog_post WHERE publish_status='PUBLISHED' ORDER BY published_at DESC,id DESC LIMIT 20");
        System.out.println("EXPERIENCE_EXPLAIN " + explain);
    }

    private void measure(String name, Supplier<?> work) {
        statements.set(0);
        long start = System.nanoTime(); work.get();
        double cold = (System.nanoTime() - start) / 1_000_000.0;
        int firstSql = statements.get();
        List<Double> elapsed = new ArrayList<>();
        int sql = 0;
        for (int i = 0; i < 30; i++) {
            statements.set(0); start = System.nanoTime(); work.get();
            elapsed.add((System.nanoTime() - start) / 1_000_000.0); sql += statements.get();
        }
        Collections.sort(elapsed);
        System.out.printf(Locale.ROOT, "EXPERIENCE_METRIC %s first_ms=%.2f first_jdbc=%d warm_p50_ms=%.2f warm_p95_ms=%.2f warm_jdbc=%.1f%n", name, cold, firstSql, elapsed.get(14), elapsed.get(28), sql / 30.0);
        statements.remove();
    }

    @TestConfiguration
    static class ProbeConfig {
        @Bean static BeanPostProcessor probeDataSource() {
            return new BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean, String name) {
                    if (!(bean instanceof DataSource source)) return bean;
                    return new DelegatingDataSource(source) {
                        @Override public Connection getConnection() throws SQLException { return connection(super.getConnection()); }
                        @Override public Connection getConnection(String user, String password) throws SQLException { return connection(super.getConnection(user, password)); }
                    };
                }
            };
        }
        static Connection connection(Connection delegate) {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                Object value = invoke(delegate, method, args);
                if (!(value instanceof Statement statement)) return value;
                Class<?> type = statement instanceof CallableStatement ? CallableStatement.class : statement instanceof PreparedStatement ? PreparedStatement.class : Statement.class;
                return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, a) -> {
                    if (m.getName().startsWith("execute")) statements.set(statements.get() + 1);
                    return invoke(statement, m, a);
                });
            });
        }
        static Object invoke(Object target, Method method, Object[] args) throws Throwable {
            try { return method.invoke(target, args); }
            catch (InvocationTargetException error) { throw error.getCause(); }
        }
    }
}
