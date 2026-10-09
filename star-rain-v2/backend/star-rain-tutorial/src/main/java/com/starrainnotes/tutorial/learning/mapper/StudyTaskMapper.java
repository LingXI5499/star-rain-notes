package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.StudyTaskEntity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 学习任务的全部 SQL 都在 mapper/tutorial/StudyTaskMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface StudyTaskMapper {

    /* 计划重算时按唯一键幂等补行，重复生成不会产生第二条任务。 */
    int insertPlanned(@Param("planId") Long planId, @Param("accountId") Long accountId,
                      @Param("tutorialId") Long tutorialId, @Param("chapterId") Long chapterId,
                      @Param("taskDate") LocalDate taskDate, @Param("sequenceNo") int sequenceNo,
                      @Param("version") int version);

    int markOverdue(@Param("accountId") Long accountId, @Param("today") LocalDate today);

    int start(@Param("taskId") Long taskId, @Param("accountId") Long accountId);

    int complete(@Param("taskId") Long taskId, @Param("accountId") Long accountId);

    int skip(@Param("taskId") Long taskId, @Param("accountId") Long accountId);

    StudyTaskEntity selectById(@Param("id") Long id);

    List<StudyTaskEntity> listByPlanId(@Param("planId") Long planId);

    List<StudyTaskEntity> listCompletedByPlanId(@Param("planId") Long planId);

    List<StudyTaskEntity> listDueByAccountId(@Param("accountId") Long accountId,
                                             @Param("today") LocalDate today);

    /* 计划重算：未完成的任务整体作废，已完成的任务保留。 */
    int deleteUnfinishedByPlanId(@Param("planId") Long planId);

    /* 结束计划：把未完成任务标记为跳过。 */
    int skipUnfinishedByPlanId(@Param("planId") Long planId);
}
