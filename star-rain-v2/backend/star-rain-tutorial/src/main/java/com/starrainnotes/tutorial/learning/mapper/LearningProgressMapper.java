package com.starrainnotes.tutorial.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.learning.entity.LearningProgressEntity;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LearningProgressMapper extends BaseMapper<LearningProgressEntity> {
    @Insert("""
            INSERT INTO sr_learning_progress
                (account_id, tutorial_id, group_id, chapter_id, scroll_anchor,
                 progress_ratio, study_seconds_total, last_studied_at)
            VALUES (#{accountId}, #{tutorialId}, #{groupId}, #{chapterId}, #{anchor},
                    #{ratio}, #{seconds}, UTC_TIMESTAMP(3))
            ON DUPLICATE KEY UPDATE
                scroll_anchor = VALUES(scroll_anchor),
                progress_ratio = VALUES(progress_ratio),
                study_seconds_total = study_seconds_total + VALUES(study_seconds_total),
                last_studied_at = VALUES(last_studied_at)
            """)
    int upsertReading(@Param("accountId") Long accountId,
                      @Param("tutorialId") Long tutorialId,
                      @Param("groupId") Long groupId,
                      @Param("chapterId") Long chapterId,
                      @Param("anchor") String anchor,
                      @Param("ratio") BigDecimal ratio,
                      @Param("seconds") int seconds);

    @Insert("""
            INSERT IGNORE INTO sr_learning_progress
                (account_id, tutorial_id, group_id, chapter_id,
                 study_seconds_total, last_studied_at)
            VALUES (#{accountId}, #{tutorialId}, #{groupId}, #{chapterId},
                    0, UTC_TIMESTAMP(3))
            """)
    int ensureRow(@Param("accountId") Long accountId,
                  @Param("tutorialId") Long tutorialId,
                  @Param("groupId") Long groupId,
                  @Param("chapterId") Long chapterId);

    @Update("""
            UPDATE sr_learning_progress
            SET completed_at = UTC_TIMESTAMP(3), last_studied_at = UTC_TIMESTAMP(3)
            WHERE account_id = #{accountId} AND chapter_id = #{chapterId}
              AND completed_at IS NULL
            """)
    int markCompleted(@Param("accountId") Long accountId,
                      @Param("chapterId") Long chapterId);
}
