package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.ReviewScheduleEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 复习计划的全部 SQL 都在 mapper/tutorial/ReviewScheduleMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface ReviewScheduleMapper {

    /* 同一账户同一卡片只有一条计划，首次学习时建立。 */
    int ensure(@Param("accountId") Long accountId, @Param("cardId") Long cardId,
               @Param("days") int days, @Param("nextAt") LocalDateTime nextAt);

    List<ReviewScheduleEntity> dueUnqueued(@Param("now") LocalDateTime now);

    List<ReviewScheduleEntity> dueUnqueuedForAccount(
            @Param("accountId") Long accountId, @Param("now") LocalDateTime now);

    ReviewScheduleEntity selectById(@Param("id") Long id);

    /* 复习完成后推进步骤、间隔与下次复习时间。 */
    int updateAfterReview(@Param("id") Long id, @Param("stepIndex") int stepIndex,
                          @Param("currentIntervalDays") int currentIntervalDays,
                          @Param("nextReviewAt") LocalDateTime nextReviewAt,
                          @Param("lastResultAt") LocalDateTime lastResultAt);
}
