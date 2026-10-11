/*
 * AnalyticsMapper：访问统计模块的数据访问文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：AnalyticsEventEntity。
 * 阅读入口：insert（写入统计）；deleteSiteDay（删除站点日）；insertSiteDay（写入站点日）；deleteContentDay（删除内容日）。
 */

package com.starrainnotes.analytics.mapper;

import com.starrainnotes.analytics.entity.AnalyticsEventEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 统计数据映射，属于访问统计模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface AnalyticsMapper {

    /**
     * 写入统计。
     *
     * 持久化操作：新增，涉及 <code>sr_analytics_event</code>。
     *
     * @param event 事件；类型为 <code>AnalyticsEventEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(AnalyticsEventEntity event);

    /**
     * 删除站点日。
     *
     * 持久化操作：删除，涉及 <code>sr_analytics_daily_site</code>。
     *
     * @param date 日期；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("date")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteSiteDay(@Param("date") LocalDate date);

    /**
     * 写入站点日。
     *
     * 持久化操作：新增，涉及 <code>sr_analytics_daily_site</code>、<code>sr_analytics_event</code>。
     *
     * @param date 日期；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("date")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertSiteDay(@Param("date") LocalDate date);

    /**
     * 删除内容日。
     *
     * 持久化操作：删除，涉及 <code>sr_analytics_daily_content</code>。
     *
     * @param date 日期；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("date")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteContentDay(@Param("date") LocalDate date);

    /**
     * 写入内容日。
     *
     * 持久化操作：新增，涉及 <code>sr_analytics_daily_content</code>、<code>sr_analytics_event</code>。
     *
     * @param date 日期；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("date")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertContentDay(@Param("date") LocalDate date);

    /**
     * 删除Referrer日。
     *
     * 持久化操作：删除，涉及 <code>sr_analytics_daily_referrer</code>。
     *
     * @param date 日期；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("date")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteReferrerDay(@Param("date") LocalDate date);

    /**
     * 写入Referrer日。
     *
     * 持久化操作：新增，涉及 <code>sr_analytics_daily_referrer</code>、<code>sr_analytics_event</code>。
     *
     * @param date 日期；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("date")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertReferrerDay(@Param("date") LocalDate date);

    /**
     * historicalTotals处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_analytics_daily_site</code>。
     *
     * @param today today；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("today")</code>
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
    Map<String, Object> historicalTotals(@Param("today") LocalDate today);

    /**
     * 日数量集合处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_analytics_event</code>。
     *
     * @param today today；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("today")</code>
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
    Map<String, Object> dayCounts(@Param("today") LocalDate today);

    /**
     * dailyTrend处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_analytics_daily_site</code>。
     *
     * @param start 开始；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("start")</code>
     * @param end 结束；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("end")</code>
     * @param today today；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("today")</code>
     * @return 本次处理得到的条目集合
     */
    List<Map<String, Object>> dailyTrend(@Param("start") LocalDate start,
        @Param("end") LocalDate end, @Param("today") LocalDate today);

    /**
     * hot内容处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_analytics_daily_content</code>、<code>sr_analytics_event</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param start 开始；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("start")</code>
     * @param end 结束；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("end")</code>
     * @param today today；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("today")</code>
     * @param type 类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("type")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 符合 SQL 条件的记录数量
     */
    List<Map<String, Object>> hotContent(@Param("start") LocalDate start,
        @Param("end") LocalDate end, @Param("today") LocalDate today,
        @Param("type") String type, @Param("limit") int limit);

    /**
     * referrers处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_analytics_daily_referrer</code>、<code>sr_analytics_event</code>。
     *
     * @param start 开始；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("start")</code>
     * @param end 结束；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("end")</code>
     * @param today today；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("today")</code>
     * @return 符合 SQL 条件的记录数量
     */
    List<Map<String, Object>> referrers(@Param("start") LocalDate start,
        @Param("end") LocalDate end, @Param("today") LocalDate today);

    /**
     * 删除原始之前。
     *
     * 持久化操作：删除，涉及 <code>sr_analytics_event</code>。
     *
     * @param cutoff cutoff；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("cutoff")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteRawBefore(@Param("cutoff") LocalDate cutoff);
}
