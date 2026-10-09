package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.ReviewTaskEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 复习任务的全部 SQL 都在 mapper/tutorial/ReviewTaskMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface ReviewTaskMapper {

    /* 同一复习计划同一到期时间只生成一条任务，重复入队由唯一键挡住。 */
    int insertDue(@Param("scheduleId") Long scheduleId, @Param("accountId") Long accountId,
                  @Param("cardId") Long cardId, @Param("dueAt") LocalDateTime dueAt);

    int markOverdue(@Param("accountId") Long accountId, @Param("now") LocalDateTime now);

    int complete(@Param("taskId") Long taskId, @Param("accountId") Long accountId);

    ReviewTaskEntity selectById(@Param("id") Long id);

    List<ReviewTaskEntity> listPendingByAccountId(@Param("accountId") Long accountId);

    long countCompletedByAccountId(@Param("accountId") Long accountId);
}
