package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import com.starrainnotes.seo.entity.SeoNotificationLog;
import com.starrainnotes.seo.mapper.SeoNotificationMapper;
import com.starrainnotes.seo.mapper.SeoPageMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * seo 模块 2 个 Mapper（SeoPageMapper、SeoNotificationMapper）的真实数据库读写验证。
 *
 * SeoPageMapper 的三条读取各有不同的列清单（active 全列、activeMeta 只取元信息、
 * activePages 只取 route_path/canonical_url/generated_at），迁移时最容易把某一条的列抄错，
 * 而 resultType 是 VO，抄错只表现为字段为 null。所以这里把「该有的字段有值」和
 * 「这条语句本来就不该取的字段是 null」两头都钉住。
 *
 * SeoNotificationMapper 的实体只声明了 6 个字段，而 claim / success / failure 会写
 * attempt_count / http_status / last_error / last_attempt_at —— 这些列用 Mapper 读不回来，
 * 所以本类在同一个未提交事务里用一条 SELECT 直接核对写路径的结果，
 * 而不是因为「实体没这个字段」就把断言降到只断影响行数。
 *
 * enqueue 的 `<insert>` 没有配 useGeneratedKeys（本项目其它 insert 都配了），
 * 所以实体上的 id 不会被回填；SeoNotificationService 只在 processPending 里用从 due()
 * 读回来的 id，这条路径本身是好的，因此这里不依赖回填，而是用 due() 把刚入队的行读回来，
 * 顺带验证 due 的列映射。该 XML 不一致已记入交付说明（未改生产代码）。
 *
 * 通知重试的 5 次上限靠传入不同的 now 推进（claim 的 WHERE 要求上次尝试早于 now-5min），
 * 全程不睡觉、不看数据库时钟。
 *
 * 全程在一个不 commit 的 SqlSession 里跑，@AfterEach 回滚；最后一个用例用新连接复核。
 */
class SeoMapperXmlTest extends MapperXmlIntegrationSupport {

    private static final AtomicLong PROBE = new AtomicLong(System.nanoTime() % 1_000_000L);

    private SqlSession session;
    private SeoPageMapper pages;
    private SeoNotificationMapper notifications;

    @BeforeEach
    void open() {
        session = openSession();
        pages = session.getMapper(SeoPageMapper.class);
        notifications = session.getMapper(SeoNotificationMapper.class);
    }

    @AfterEach
    void rollback() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    @Test
    void everySeoMapperIsReachableThroughTheSession() {
        assertNotNull(pages);
        assertNotNull(notifications);
    }

    @Test
    void upsertedSnapshotRoundTripsThroughTheFullColumnList() {
        String routePath = route();
        SeoPageSnapshot snapshot = newSnapshot(routePath, "TUTORIAL", probeId());

        assertEquals(1, pages.upsert(snapshot), "首次 upsert 应当插入一行");

        SeoPageSnapshot active = pages.active(routePath);
        assertNotNull(active, "active 读不到刚提交的快照");
        assertEquals(routePath, active.getRoutePath(), "别名 routePath/route_path 映射不上");
        assertEquals("TUTORIAL", active.getContentType());
        assertEquals(snapshot.getContentId(), active.getContentId());
        assertEquals(snapshot.getCanonicalUrl(), active.getCanonicalUrl());
        assertEquals(snapshot.getTitle(), active.getTitle());
        assertEquals(snapshot.getDescription(), active.getDescription());
        assertEquals("index,follow", active.getRobotsDirective());
        assertTrue(active.getStructuredDataJson().contains("mapper-xml-test"),
                "structured_data_json 没有按字符串映射回来：" + active.getStructuredDataJson());
        assertEquals(snapshot.getHtmlSnapshot(), active.getHtmlSnapshot());
        assertEquals(snapshot.getSourceVersionRef(), active.getSourceVersionRef());
        assertEquals("ACTIVE", active.getStatus());
        assertNotNull(active.getGeneratedAt(), "generated_at 由列默认值填充，应能读回");

        assertTrue(pages.activeCount() >= 1);
        assertTrue(pages.activePaths().contains(routePath));
        assertTrue(pages.activePages().stream().anyMatch(page -> routePath.equals(page.getRoutePath())),
                "activePages 里找不到刚提交的路由");
        assertNull(pages.active(route() + "-missing"), "不存在的路由应返回 null");
    }

    @Test
    void activeMetaOnlyCarriesTheMetadataColumns() {
        String routePath = route();
        SeoPageSnapshot snapshot = newSnapshot(routePath, "BLOG", probeId());
        pages.upsert(snapshot);

        SeoPageSnapshot meta = pages.activeMeta(routePath);
        assertNotNull(meta, "activeMeta 读不到刚提交的快照");
        assertEquals(routePath, meta.getRoutePath(), "别名 routePath 丢失");
        assertEquals(snapshot.getCanonicalUrl(), meta.getCanonicalUrl(), "别名 canonicalUrl 丢失");
        assertEquals(snapshot.getTitle(), meta.getTitle(), "别名 title 丢失");
        assertEquals(snapshot.getDescription(), meta.getDescription(), "别名 description 丢失");
        assertEquals("index,follow", meta.getRobotsDirective(), "别名 robotsDirective 丢失");
        // 这条语句的列清单里本来就没有这几列，读回来必须是 null（用来证明列清单与 active 确实不同）
        assertNull(meta.getContentType());
        assertNull(meta.getContentId());
        assertNull(meta.getHtmlSnapshot());
        assertNull(meta.getStatus());
        assertNull(meta.getGeneratedAt());
    }

    @Test
    void reindexingTheSameRouteUpdatesInPlaceAndKeepsOneActiveRow() {
        String routePath = route();
        SeoPageSnapshot first = newSnapshot(routePath, "TUTORIAL", probeId());
        assertEquals(1, pages.upsert(first));

        SeoPageSnapshot second = newSnapshot(routePath, "TUTORIAL", first.getContentId());
        second.setTitle(first.getTitle() + "-改名");
        second.setDescription("XML 验证描述（改）");
        second.setRobotsDirective("noindex,nofollow");
        second.setHtmlSnapshot("<html>mapper-xml-test-改</html>");
        assertEquals(2, pages.upsert(second), "ON DUPLICATE KEY UPDATE 真正改动行时 MySQL 返回 2");

        long before = pages.activeCount();
        SeoPageSnapshot active = pages.active(routePath);
        assertEquals(second.getTitle(), active.getTitle(), "标题没有被更新");
        assertEquals("noindex,nofollow", active.getRobotsDirective(), "robots_directive 没有被更新");
        assertTrue(active.getHtmlSnapshot().contains("mapper-xml-test-改"));
        assertEquals(before, pages.activeCount(), "同路由重复索引不应产生第二行");
    }

    @Test
    void activePathsByPrefixAndRemoveFollowTheRoutePath() {
        String prefix = "/mapper-xml-test/" + unique("seoprefix");
        String child = prefix + "/child";
        String other = "/mapper-xml-test/" + unique("seoother") + "/page";
        pages.upsert(newSnapshot(child, "TUTORIAL", probeId()));
        pages.upsert(newSnapshot(other, "TUTORIAL", probeId()));

        List<String> byPrefix = pages.activePathsByPrefix(prefix);
        assertTrue(byPrefix.contains(child), "按前缀取路由没生效");
        assertFalse(byPrefix.contains(other), "前缀匹配把不该命中的路由也带上了");

        assertEquals(1, pages.remove(child));
        assertNull(pages.active(child), "REMOVED 的快照不应被 active 读到");
        assertNull(pages.activeMeta(child));
        assertFalse(pages.activePaths().contains(child), "REMOVED 的路由仍在 activePaths 里");
        assertTrue(pages.activePathsByPrefix(prefix).isEmpty(), "REMOVED 的路由仍在按前缀查询里");
        assertNotNull(pages.active(other), "删一条路由不应影响别的路由");
        assertEquals(0, pages.remove(prefix + "-no-such-route"));
    }

    @Test
    void enqueueThenClaimMovesTheRowToProcessing() {
        LocalDateTime enqueuedAt = now();
        SeoNotificationLog pending = enqueueAndReload(newLog(unique("mapperxmlseo")),
                enqueuedAt.plusMinutes(1));

        assertEquals("PENDING", pending.getStatus());
        assertEquals(0, pending.getAttemptCount().intValue());
        assertNotNull(pending.getNextRetryAt(), "enqueue 必须写入 next_retry_at，否则不会被 due 取到");
        assertEquals("INDEXNOW", pending.getProviderCode());
        assertEquals("UPDATED", pending.getChangeType());
        assertNotNull(pending.getCanonicalUrl());

        LocalDateTime claimedAt = enqueuedAt.plusMinutes(1);
        assertEquals(1, notifications.claim(pending.getId(), claimedAt));
        assertEquals(0, notifications.claim(pending.getId(), claimedAt),
                "同一次尝试里不能被 claim 两次（PROCESSING 且未超过 5 分钟）");
        assertEquals(1, ((Number) column("attempt_count", pending.getId())).intValue(),
                "attempt_count 没有累加");
        assertEquals("PROCESSING", column("status", pending.getId()));
        assertEquals(claimedAt, column("last_attempt_at", pending.getId()),
                "claim 必须记下本次尝试时间");

        assertEquals(0, notifications.claim(-1L, claimedAt));
    }

    @Test
    void claimGivesUpAfterFiveAttempts() {
        LocalDateTime base = now();
        SeoNotificationLog row = enqueueAndReload(newLog(unique("mapperxmlseo-retry")),
                base.plusMinutes(1));
        long id = row.getId();

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertEquals(1, notifications.claim(id, base.plusMinutes(attempt * 6L)),
                    "第 " + attempt + " 次 claim 应当成功");
        }
        assertEquals(5, ((Number) column("attempt_count", id)).intValue());
        assertEquals("PROCESSING", column("status", id));

        assertEquals(0, notifications.claim(id, base.plusMinutes(60)),
                "attempt_count 到 5 以后必须放弃，不能再 claim");
        assertEquals(5, ((Number) column("attempt_count", id)).intValue(),
                "被拒绝的 claim 不应改动计数");
        assertTrue(notifications.due(base.plusMinutes(60), 50).stream()
                        .noneMatch(item -> id == item.getId()),
                "attempt_count 到 5 以后不应再出现在 due 里");
    }

    @Test
    void successStoresTheHttpStatusAndClearsTheRetryColumns() {
        LocalDateTime base = now();
        SeoNotificationLog row = enqueueAndReload(newLog(unique("mapperxmlseo-success")),
                base.plusMinutes(1));
        assertEquals(1, notifications.claim(row.getId(), base.plusMinutes(1)));

        assertEquals(1, notifications.success(row.getId(), 204));
        assertEquals("SUCCESS", column("status", row.getId()));
        assertEquals(204, ((Number) column("http_status", row.getId())).intValue());
        assertNull(column("last_error", row.getId()));
        assertNull(column("next_retry_at", row.getId()));
        assertTrue(notifications.due(base.plusMinutes(10), 50).stream()
                        .noneMatch(item -> row.getId().equals(item.getId())),
                "已成功的通知不应再出现在 due 里");
        assertEquals(0, notifications.success(-1L, 200));
    }

    @Test
    void failureStoresTheErrorAndSchedulesTheNextRetry() {
        LocalDateTime base = now();
        SeoNotificationLog row = enqueueAndReload(newLog(unique("mapper-seo-failure")),
                base.plusMinutes(1));
        assertEquals(1, notifications.claim(row.getId(), base.plusMinutes(1)));

        LocalDateTime nextRetryAt = base.plusMinutes(3);
        assertEquals(1, notifications.failure(row.getId(), 503, "upstream unavailable", nextRetryAt));
        assertEquals("FAILED", column("status", row.getId()));
        assertEquals(503, ((Number) column("http_status", row.getId())).intValue());
        assertEquals("upstream unavailable", column("last_error", row.getId()));
        assertNotNull(column("next_retry_at", row.getId()), "failure 必须安排下一次重试时间");

        assertTrue(notifications.due(base.plusMinutes(2), 50).stream()
                        .noneMatch(item -> row.getId().equals(item.getId())),
                "还没到重试时间的失败通知不应出现在 due 里");
        SeoNotificationLog retried = notifications.due(nextRetryAt.plusSeconds(1), 50).stream()
                .filter(item -> row.getId().equals(item.getId())).findFirst().orElse(null);
        assertNotNull(retried, "到点后的失败通知应当重新出现在 due 里");
        assertEquals(1, retried.getAttemptCount().intValue(), "重试次数应当保留");
        assertEquals(0, notifications.failure(-1L, 500, "boom", nextRetryAt));
    }

    @Test
    void rollbackLeavesNoSeoProbeRowsBehind() {
        String routePath = route();
        pages.upsert(newSnapshot(routePath, "TUTORIAL", probeId()));
        SeoNotificationLog row = enqueueAndReload(newLog(unique("mapper-seo-rollback")), now().plusMinutes(1));

        session.rollback();
        try (SqlSession fresh = openSession()) {
            assertNull(fresh.getMapper(SeoPageMapper.class).active(routePath),
                    "回滚后不应在开发库里留下测试 SEO 快照");
            assertTrue(fresh.getMapper(SeoNotificationMapper.class).due(now().plusDays(1), 500).stream()
                            .noneMatch(item -> row.getId().equals(item.getId())),
                    "回滚后不应在开发库里留下测试 SEO 通知");
        }
    }

    /*
     * enqueue 原先漏了 useGeneratedKeys（本项目其它 insert 都配了），入队后实体上的 id 恒为 null；
     * 这是本测试发现的缺陷，已补上属性，所以现在直接断言主键被回填。
     * 随后仍用 due() 把行读回来（顺带验证 due 的列映射与状态条件）。
     */
    private SeoNotificationLog enqueueAndReload(SeoNotificationLog log, LocalDateTime dueAt) {
        assertEquals(1, notifications.enqueue(log));
        assertNotNull(log.getId(), "enqueue 应回填自增主键（缺 useGeneratedKeys 时这里会是 null）");
        List<SeoNotificationLog> due = notifications.due(dueAt, 50);
        SeoNotificationLog row = due.stream()
                .filter(item -> log.getId().equals(item.getId()))
                .findFirst().orElse(null);
        assertNotNull(row, "刚入队的通知应当立刻能被 due 取到（检查 next_retry_at 与状态条件）");
        return row;
    }

    /* SeoNotificationLog 实体没有声明 http_status / last_error / last_attempt_at，直接读库核对 */
    private Object column(String column, Long id) {
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "SELECT " + column + " FROM sr_seo_notification_log WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next(), "SEO 通知行不见了");
                return rows.getObject(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("读取 sr_seo_notification_log." + column + " 失败", exception);
        }
    }

    private SeoPageSnapshot newSnapshot(String routePath, String contentType, Long contentId) {
        SeoPageSnapshot snapshot = new SeoPageSnapshot();
        snapshot.setRoutePath(routePath);
        snapshot.setContentType(contentType);
        snapshot.setContentId(contentId);
        snapshot.setCanonicalUrl("https://example.test" + routePath);
        snapshot.setTitle("XML 验证标题 " + unique("seo"));
        snapshot.setDescription("XML 验证描述");
        snapshot.setRobotsDirective("index,follow");
        snapshot.setStructuredDataJson("{\"marker\":\"mapper-xml-test\"}");
        snapshot.setHtmlSnapshot("<html>mapper-xml-test</html>");
        snapshot.setSourceVersionRef("mapper-xml-test-ref-" + PROBE.incrementAndGet());
        return snapshot;
    }

    private SeoNotificationLog newLog(String marker) {
        SeoNotificationLog log = new SeoNotificationLog();
        log.setProviderCode("INDEXNOW");
        log.setRoutePath("/mapper-xml-test/" + marker);
        log.setCanonicalUrl("https://example.test/mapper-xml-test/" + marker);
        log.setChangeType("UPDATED");
        return log;
    }

    private String route() {
        return "/mapper-xml-test/" + unique("seo") + "/page";
    }

    /* 逻辑外键，库里没有物理 FOREIGN KEY：用一个大号段避免和真实内容撞号 */
    private long probeId() {
        return 900_000_000_000L + PROBE.incrementAndGet();
    }

    private String unique(String prefix) {
        return prefix + "-" + PROBE.incrementAndGet() + "-" + System.nanoTime();
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
