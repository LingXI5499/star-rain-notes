package com.starrainnotes.analytics.mapper;

import com.starrainnotes.analytics.entity.AnalyticsEventEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnalyticsMapper {

    int insert(AnalyticsEventEntity event);

    int deleteSiteDay(@Param("date") LocalDate date);

    int insertSiteDay(@Param("date") LocalDate date);

    int deleteContentDay(@Param("date") LocalDate date);

    int insertContentDay(@Param("date") LocalDate date);

    int deleteReferrerDay(@Param("date") LocalDate date);

    int insertReferrerDay(@Param("date") LocalDate date);

    Map<String, Object> historicalTotals(@Param("today") LocalDate today);

    Map<String, Object> dayCounts(@Param("today") LocalDate today);

    List<Map<String, Object>> dailyTrend(@Param("start") LocalDate start,
        @Param("end") LocalDate end, @Param("today") LocalDate today);

    List<Map<String, Object>> hotContent(@Param("start") LocalDate start,
        @Param("end") LocalDate end, @Param("today") LocalDate today,
        @Param("type") String type, @Param("limit") int limit);

    List<Map<String, Object>> referrers(@Param("start") LocalDate start,
        @Param("end") LocalDate end, @Param("today") LocalDate today);

    int deleteRawBefore(@Param("cutoff") LocalDate cutoff);
}
