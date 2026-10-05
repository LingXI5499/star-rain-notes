package com.starrainnotes.tutorial.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.learning.entity.ReviewScheduleEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewScheduleMapper extends BaseMapper<ReviewScheduleEntity> {

    int ensure(@Param("accountId") Long accountId, @Param("cardId") Long cardId,
               @Param("days") int days, @Param("nextAt") LocalDateTime nextAt);

    java.util.List<ReviewScheduleEntity> dueUnqueued(@Param("now") LocalDateTime now);

    java.util.List<ReviewScheduleEntity> dueUnqueuedForAccount(
            @Param("accountId") Long accountId, @Param("now") LocalDateTime now);
}
