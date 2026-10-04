package com.starrainnotes.seo.mapper;

import com.starrainnotes.seo.notify.SeoNotificationLog;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SeoNotificationMapper {
    int enqueue(SeoNotificationLog log);
    List<SeoNotificationLog> due(@Param("now") LocalDateTime now, @Param("limit") int limit);
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now);
    int success(@Param("id") Long id, @Param("httpStatus") Integer httpStatus);
    int failure(@Param("id") Long id, @Param("httpStatus") Integer httpStatus,
        @Param("error") String error, @Param("nextRetryAt") LocalDateTime nextRetryAt);
}
