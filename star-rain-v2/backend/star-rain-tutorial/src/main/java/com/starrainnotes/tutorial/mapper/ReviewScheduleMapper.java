package com.starrainnotes.tutorial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.entity.ReviewScheduleEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewScheduleMapper extends BaseMapper<ReviewScheduleEntity> {
    @Insert("""
            INSERT IGNORE INTO sr_review_schedule
                (account_id, knowledge_card_id, step_index, current_interval_days,
                 next_review_at, status)
            VALUES (#{accountId}, #{cardId}, 0, #{days}, #{nextAt}, 'ACTIVE')
            """)
    int ensure(@Param("accountId") Long accountId, @Param("cardId") Long cardId,
               @Param("days") int days, @Param("nextAt") LocalDateTime nextAt);

    @Select("""
            SELECT s.id, s.account_id, s.knowledge_card_id, s.step_index,
                   s.current_interval_days, s.next_review_at, s.status,
                   s.last_result_at, s.created_at, s.updated_at
            FROM sr_review_schedule s
            WHERE s.status = 'ACTIVE' AND s.next_review_at <= #{now}
              AND NOT EXISTS (SELECT 1 FROM sr_review_task t
                  WHERE t.schedule_id = s.id AND t.due_at = s.next_review_at)
            ORDER BY s.next_review_at, s.id LIMIT 500
            """)
    java.util.List<ReviewScheduleEntity> dueUnqueued(@Param("now") LocalDateTime now);

    @Select("""
            SELECT s.id, s.account_id, s.knowledge_card_id, s.step_index,
                   s.current_interval_days, s.next_review_at, s.status,
                   s.last_result_at, s.created_at, s.updated_at
            FROM sr_review_schedule s
            WHERE s.account_id = #{accountId} AND s.status = 'ACTIVE'
              AND s.next_review_at <= #{now}
              AND NOT EXISTS (SELECT 1 FROM sr_review_task t
                  WHERE t.schedule_id = s.id AND t.due_at = s.next_review_at)
            ORDER BY s.next_review_at, s.id LIMIT 500
            """)
    java.util.List<ReviewScheduleEntity> dueUnqueuedForAccount(
            @Param("accountId") Long accountId, @Param("now") LocalDateTime now);
}
