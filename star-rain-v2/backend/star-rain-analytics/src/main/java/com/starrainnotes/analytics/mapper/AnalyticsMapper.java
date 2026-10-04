package com.starrainnotes.analytics.mapper;

import com.starrainnotes.analytics.entity.AnalyticsEventEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AnalyticsMapper {
    @Insert("""
        INSERT INTO sr_analytics_event(event_type, route_key, content_type, content_id,
            referrer_category, referrer_host)
        VALUES (#{eventType}, #{routeKey}, #{contentType}, #{contentId},
            #{referrerCategory}, #{referrerHost})
        """)
    int insert(AnalyticsEventEntity event);

    @Delete("DELETE FROM sr_analytics_daily_site WHERE stat_date = #{date}")
    int deleteSiteDay(@Param("date") LocalDate date);

    @Insert("""
        INSERT INTO sr_analytics_daily_site(stat_date, page_view_count, content_view_count)
        SELECT #{date},
          SUM(CASE WHEN event_type = 'PAGE_VIEW' THEN 1 ELSE 0 END),
          SUM(CASE WHEN event_type = 'CONTENT_VIEW' THEN 1 ELSE 0 END)
        FROM sr_analytics_event
        WHERE occurred_at >= #{date} AND occurred_at < DATE_ADD(#{date}, INTERVAL 1 DAY)
        HAVING COUNT(*) > 0
        """)
    int insertSiteDay(@Param("date") LocalDate date);

    @Delete("DELETE FROM sr_analytics_daily_content WHERE stat_date = #{date}")
    int deleteContentDay(@Param("date") LocalDate date);

    @Insert("""
        INSERT INTO sr_analytics_daily_content(stat_date, content_type, content_id, view_count)
        SELECT #{date}, content_type, content_id, COUNT(*)
        FROM sr_analytics_event
        WHERE event_type = 'CONTENT_VIEW' AND occurred_at >= #{date}
          AND occurred_at < DATE_ADD(#{date}, INTERVAL 1 DAY)
        GROUP BY content_type, content_id
        """)
    int insertContentDay(@Param("date") LocalDate date);

    @Delete("DELETE FROM sr_analytics_daily_referrer WHERE stat_date = #{date}")
    int deleteReferrerDay(@Param("date") LocalDate date);

    @Insert("""
        INSERT INTO sr_analytics_daily_referrer(stat_date, referrer_category, view_count)
        SELECT #{date}, referrer_category, COUNT(*) FROM sr_analytics_event
        WHERE occurred_at >= #{date} AND occurred_at < DATE_ADD(#{date}, INTERVAL 1 DAY)
        GROUP BY referrer_category
        """)
    int insertReferrerDay(@Param("date") LocalDate date);

    @Select("""
        SELECT COALESCE(SUM(page_view_count),0) AS pageViews,
          COALESCE(SUM(content_view_count),0) AS contentViews
        FROM sr_analytics_daily_site WHERE stat_date < #{today}
        """)
    Map<String, Object> historicalTotals(@Param("today") LocalDate today);

    @Select("""
        SELECT COALESCE(SUM(event_type = 'PAGE_VIEW'),0) AS pageViews,
          COALESCE(SUM(event_type = 'CONTENT_VIEW'),0) AS contentViews
        FROM sr_analytics_event WHERE occurred_at >= #{today}
          AND occurred_at < DATE_ADD(#{today}, INTERVAL 1 DAY)
        """)
    Map<String, Object> dayCounts(@Param("today") LocalDate today);

    @Select("""
        SELECT stat_date AS statDate, page_view_count AS pageViews,
          content_view_count AS contentViews FROM sr_analytics_daily_site
        WHERE stat_date >= #{start} AND stat_date <= #{end} AND stat_date < #{today}
        ORDER BY stat_date
        """)
    List<Map<String, Object>> dailyTrend(@Param("start") LocalDate start,
        @Param("end") LocalDate end, @Param("today") LocalDate today);

    @Select("""
        SELECT content_type AS contentType, content_id AS contentId,
          SUM(view_count) AS viewCount FROM (
          SELECT content_type, content_id, view_count FROM sr_analytics_daily_content
          WHERE stat_date >= #{start} AND stat_date <= #{end} AND stat_date < #{today}
          UNION ALL
          SELECT content_type, content_id, COUNT(*) AS view_count FROM sr_analytics_event
          WHERE event_type = 'CONTENT_VIEW' AND #{today} BETWEEN #{start} AND #{end}
            AND occurred_at >= #{today} AND occurred_at < DATE_ADD(#{today}, INTERVAL 1 DAY)
          GROUP BY content_type, content_id
        ) counts WHERE (#{type} IS NULL OR content_type = #{type})
        GROUP BY content_type, content_id ORDER BY viewCount DESC LIMIT #{limit}
        """)
    List<Map<String, Object>> hotContent(@Param("start") LocalDate start,
        @Param("end") LocalDate end, @Param("today") LocalDate today,
        @Param("type") String type, @Param("limit") int limit);

    @Select("""
        SELECT referrer_category AS category, SUM(view_count) AS viewCount FROM (
          SELECT referrer_category, view_count FROM sr_analytics_daily_referrer
          WHERE stat_date >= #{start} AND stat_date <= #{end} AND stat_date < #{today}
          UNION ALL
          SELECT referrer_category, COUNT(*) AS view_count FROM sr_analytics_event
          WHERE #{today} BETWEEN #{start} AND #{end} AND occurred_at >= #{today}
            AND occurred_at < DATE_ADD(#{today}, INTERVAL 1 DAY)
          GROUP BY referrer_category
        ) counts GROUP BY referrer_category ORDER BY viewCount DESC
        """)
    List<Map<String, Object>> referrers(@Param("start") LocalDate start,
        @Param("end") LocalDate end, @Param("today") LocalDate today);

    @Delete("DELETE FROM sr_analytics_event WHERE occurred_at < #{cutoff}")
    int deleteRawBefore(@Param("cutoff") LocalDate cutoff);
}
