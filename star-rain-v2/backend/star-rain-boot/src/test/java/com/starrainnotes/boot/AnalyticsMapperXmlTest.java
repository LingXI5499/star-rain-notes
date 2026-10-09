package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.analytics.entity.AnalyticsEventEntity;
import com.starrainnotes.analytics.mapper.AnalyticsMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * analytics 模块的真实数据库读写验证。
 *
 * 这是 P0-1 里点名要求、但此前唯一还没做的一个模块：
 * `AnalyticsMapper` 的 12 条语句是「注解 SQL 搬进 XML」那轮迁进来的，
 * 而单元测试里 Mapper 被 mock 掉，XML 从没被 MyBatis 解析过。
 *
 * 这个 Mapper 特别值得测，因为它的五个查询都返回 `Map<String, Object>` ——
 * **没有任何编译期或运行期的类型检查能发现列别名写错**：别名写错只会让
 * `map.get("pageViews")` 返回 null，页面显示 0，没人会当成 bug。
 * 所以这里逐个别名断言。
 *
 * 全程不 commit，`@AfterEach` 里 rollback；对聚合表的 DELETE 也因此不会影响开发库。
 */
class AnalyticsMapperXmlTest extends MapperXmlIntegrationSupport {

    private SqlSession session;
    private AnalyticsMapper mapper;
    private LocalDate today;

    @BeforeEach
    void open() {
        session = openSession();
        mapper = session.getMapper(AnalyticsMapper.class);
        today = LocalDate.now();
    }

    @AfterEach
    void rollback() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    @Test
    void insertAndDayCountsAgreeOnTodaysEvents() {
        long before = number(mapper.dayCounts(today), "pageViews");

        assertEquals(1, mapper.insert(pageView("/analytics-xml-test")));
        mapper.insert(contentView(424242L));

        Map<String, Object> counts = mapper.dayCounts(today);
        assertNotNull(counts, "dayCounts 必须返回一行");
        assertEquals(before + 1, number(counts, "pageViews"),
                "插入的 PAGE_VIEW 没有计入当天计数：检查 XML 的条件与别名");
        assertTrue(number(counts, "contentViews") >= 1,
                "插入的 CONTENT_VIEW 没有计入当天计数");
    }

    @Test
    void historicalTotalsExposesBothAliases() {
        Map<String, Object> totals = mapper.historicalTotals(today);
        assertNotNull(totals);
        assertTrue(totals.containsKey("pageViews"), "别名 pageViews 丢失：" + totals.keySet());
        assertTrue(totals.containsKey("contentViews"), "别名 contentViews 丢失：" + totals.keySet());
        assertTrue(number(totals, "pageViews") >= 0);
    }

    @Test
    void dailyTrendRowsUseTheAliasesTheServiceReads() {
        List<Map<String, Object>> rows = mapper.dailyTrend(today.minusDays(30), today.plusDays(1), today.plusDays(2));
        for (Map<String, Object> row : rows) {
            assertTrue(row.containsKey("statDate"), "别名 statDate 丢失：" + row.keySet());
            assertTrue(row.containsKey("pageViews"), "别名 pageViews 丢失：" + row.keySet());
            assertTrue(row.containsKey("contentViews"), "别名 contentViews 丢失：" + row.keySet());
        }
    }

    @Test
    void hotContentAliasesAndTypeFilterWork() {
        mapper.insert(contentView(424243L));

        List<Map<String, Object>> rows = mapper.hotContent(today, today, today, "TUTORIAL", 5);
        assertTrue(rows.size() <= 5, "LIMIT 没有生效");
        for (Map<String, Object> row : rows) {
            assertTrue(row.containsKey("contentType"), "别名 contentType 丢失：" + row.keySet());
            assertTrue(row.containsKey("contentId"), "别名 contentId 丢失：" + row.keySet());
            assertTrue(row.containsKey("viewCount"), "别名 viewCount 丢失：" + row.keySet());
        }

        // type 为 null 时必须走 `#{type} IS NULL` 分支返回全部类型
        List<Map<String, Object>> all = mapper.hotContent(today, today, today, null, 50);
        assertTrue(all.size() >= rows.size(), "type 为 null 的分支没有返回全部类型");
    }

    @Test
    void referrersAliasesWork() {
        mapper.insert(referredPageView("/analytics-xml-test-ref"));

        List<Map<String, Object>> rows = mapper.referrers(today, today, today);
        for (Map<String, Object> row : rows) {
            assertTrue(row.containsKey("category"), "别名 category 丢失：" + row.keySet());
            assertTrue(row.containsKey("viewCount"), "别名 viewCount 丢失：" + row.keySet());
        }
    }

    @Test
    void dailyAggregationRoundTripRebuildsTheDayFromRawEvents() {
        mapper.insert(pageView("/analytics-xml-test-agg"));
        mapper.insert(contentView(424244L));

        mapper.deleteSiteDay(today);
        assertEquals(1, mapper.insertSiteDay(today));
        mapper.deleteContentDay(today);
        mapper.insertContentDay(today);
        mapper.deleteReferrerDay(today);
        mapper.insertReferrerDay(today);

        // historicalTotals 只看 stat_date < tomorrow，正好覆盖今天这行
        Map<String, Object> totals = mapper.historicalTotals(today.plusDays(1));
        assertTrue(number(totals, "pageViews") >= 1,
                "insertSiteDay 写入的当天聚合没有被 historicalTotals 读到（检查 stat_date 与 < 条件）");

        List<Map<String, Object>> trend = mapper.dailyTrend(today, today, today.plusDays(1));
        assertEquals(1, trend.size(), "当天聚合行应当能在 dailyTrend 里查到");
        assertEquals(today.toString(), String.valueOf(trend.get(0).get("statDate")));
    }

    @Test
    void deleteRawBeforeOnlyRemovesOlderEvents() {
        mapper.insert(pageView("/analytics-xml-test-keep"));
        // 截止日期取昨天：今天的探针数据必须留下
        mapper.deleteRawBefore(today);
        assertTrue(number(mapper.dayCounts(today), "pageViews") >= 1,
                "deleteRawBefore 把当天的事件也删掉了");
    }

    /*
     * 回归：referrer_category 在库里是 NOT NULL DEFAULT 'DIRECT'，
     * 而 XML 显式写了这一列，会覆盖列默认值。如果退回成裸 #{referrerCategory}，
     * 这一条会抛非空约束异常，并且被 AnalyticsServiceImpl.safelyInsert 吞掉，
     * 表现为「页面浏览静默丢失」。
     */
    @Test
    void nullReferrerCategoryFallsBackToTheColumnDefaultInsteadOfFailing() {
        AnalyticsEventEntity anonymous = event("PAGE_VIEW", "/analytics-xml-test-null-ref", null, null, null);
        assertEquals(1, mapper.insert(anonymous),
                "referrer_category 为 null 时插入失败：XML 覆盖了库里的 DEFAULT 'DIRECT'");
    }

    private AnalyticsEventEntity pageView(String route) {
        return event("PAGE_VIEW", route, null, null, "DIRECT");
    }

    private AnalyticsEventEntity referredPageView(String route) {
        return event("PAGE_VIEW", route, null, null, "SEARCH");
    }

    private AnalyticsEventEntity contentView(Long contentId) {
        return event("CONTENT_VIEW", "/tutorials/:slug", "TUTORIAL", contentId, null);
    }

    private AnalyticsEventEntity event(String type, String route, String contentType, Long contentId, String referrer) {
        AnalyticsEventEntity entity = new AnalyticsEventEntity();
        entity.setEventType(type);
        entity.setRouteKey(route);
        entity.setContentType(contentType);
        entity.setContentId(contentId);
        entity.setReferrerCategory(referrer);
        entity.setReferrerHost(referrer == null ? null : "example.test");
        return entity;
    }

    private long number(Map<String, Object> row, String key) {
        Object value = row == null ? null : row.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
