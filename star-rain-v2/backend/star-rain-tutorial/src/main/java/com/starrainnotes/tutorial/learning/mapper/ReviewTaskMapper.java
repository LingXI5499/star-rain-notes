package com.starrainnotes.tutorial.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.learning.entity.ReviewTaskEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReviewTaskMapper extends BaseMapper<ReviewTaskEntity> {
    @Insert("""
            INSERT IGNORE INTO sr_review_task
                (schedule_id, account_id, knowledge_card_id, due_at, status)
            VALUES (#{scheduleId}, #{accountId}, #{cardId}, #{dueAt}, 'PENDING')
            """)
    int insertDue(@Param("scheduleId") Long scheduleId, @Param("accountId") Long accountId,
                  @Param("cardId") Long cardId, @Param("dueAt") LocalDateTime dueAt);

    @Update("""
            UPDATE sr_review_task SET status = 'OVERDUE'
            WHERE account_id = #{accountId} AND due_at < #{now} AND status = 'PENDING'
            """)
    int markOverdue(@Param("accountId") Long accountId, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE sr_review_task SET status = 'COMPLETED', completed_at = UTC_TIMESTAMP(3)
            WHERE id = #{taskId} AND account_id = #{accountId}
              AND status IN ('PENDING', 'OVERDUE')
            """)
    int complete(@Param("taskId") Long taskId, @Param("accountId") Long accountId);
}
