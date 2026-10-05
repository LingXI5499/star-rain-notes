package com.starrainnotes.tutorial.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.learning.entity.StudyTaskEntity;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudyTaskMapper extends BaseMapper<StudyTaskEntity> {

    int insertPlanned(@Param("planId") Long planId, @Param("accountId") Long accountId,
                      @Param("tutorialId") Long tutorialId, @Param("chapterId") Long chapterId,
                      @Param("taskDate") LocalDate taskDate, @Param("sequenceNo") int sequenceNo,
                      @Param("version") int version);

    int markOverdue(@Param("accountId") Long accountId, @Param("today") LocalDate today);

    int start(@Param("taskId") Long taskId, @Param("accountId") Long accountId);

    int complete(@Param("taskId") Long taskId, @Param("accountId") Long accountId);

    int skip(@Param("taskId") Long taskId, @Param("accountId") Long accountId);
}
