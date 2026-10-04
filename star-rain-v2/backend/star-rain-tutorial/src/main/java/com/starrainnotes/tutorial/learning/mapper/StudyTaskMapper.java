package com.starrainnotes.tutorial.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.learning.entity.StudyTaskEntity;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StudyTaskMapper extends BaseMapper<StudyTaskEntity> {
    @Insert("""
            INSERT IGNORE INTO sr_study_task
                (plan_id, account_id, tutorial_id, chapter_id, target_type, target_id,
                 task_date, sequence_no, status, generation_version)
            VALUES (#{planId}, #{accountId}, #{tutorialId}, #{chapterId}, 'CHAPTER',
                    #{chapterId}, #{taskDate}, #{sequenceNo}, 'TODO', #{version})
            """)
    int insertPlanned(@Param("planId") Long planId, @Param("accountId") Long accountId,
                      @Param("tutorialId") Long tutorialId, @Param("chapterId") Long chapterId,
                      @Param("taskDate") LocalDate taskDate, @Param("sequenceNo") int sequenceNo,
                      @Param("version") int version);

    @Update("""
            UPDATE sr_study_task SET status = 'OVERDUE'
            WHERE account_id = #{accountId} AND task_date < #{today} AND status = 'TODO'
            """)
    int markOverdue(@Param("accountId") Long accountId, @Param("today") LocalDate today);

    @Update("""
            UPDATE sr_study_task SET status = 'IN_PROGRESS', started_at = UTC_TIMESTAMP(3)
            WHERE id = #{taskId} AND account_id = #{accountId}
              AND status IN ('TODO', 'OVERDUE')
            """)
    int start(@Param("taskId") Long taskId, @Param("accountId") Long accountId);

    @Update("""
            UPDATE sr_study_task SET status = 'COMPLETED', completed_at = UTC_TIMESTAMP(3)
            WHERE id = #{taskId} AND account_id = #{accountId} AND status = 'IN_PROGRESS'
            """)
    int complete(@Param("taskId") Long taskId, @Param("accountId") Long accountId);

    @Update("""
            UPDATE sr_study_task SET status = 'SKIPPED'
            WHERE id = #{taskId} AND account_id = #{accountId}
              AND status IN ('TODO', 'IN_PROGRESS', 'OVERDUE')
            """)
    int skip(@Param("taskId") Long taskId, @Param("accountId") Long accountId);
}
