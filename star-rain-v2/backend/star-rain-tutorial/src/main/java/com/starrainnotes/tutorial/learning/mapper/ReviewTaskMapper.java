package com.starrainnotes.tutorial.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.learning.entity.ReviewTaskEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewTaskMapper extends BaseMapper<ReviewTaskEntity> {

    int insertDue(@Param("scheduleId") Long scheduleId, @Param("accountId") Long accountId,
                  @Param("cardId") Long cardId, @Param("dueAt") LocalDateTime dueAt);

    int markOverdue(@Param("accountId") Long accountId, @Param("now") LocalDateTime now);

    int complete(@Param("taskId") Long taskId, @Param("accountId") Long accountId);
}
