package com.starrainnotes.tutorial.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.learning.entity.LearningProgressEntity;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LearningProgressMapper extends BaseMapper<LearningProgressEntity> {

    int upsertReading(@Param("accountId") Long accountId,
                      @Param("tutorialId") Long tutorialId,
                      @Param("groupId") Long groupId,
                      @Param("chapterId") Long chapterId,
                      @Param("anchor") String anchor,
                      @Param("ratio") BigDecimal ratio,
                      @Param("seconds") int seconds);

    int ensureRow(@Param("accountId") Long accountId,
                  @Param("tutorialId") Long tutorialId,
                  @Param("groupId") Long groupId,
                  @Param("chapterId") Long chapterId);

    int markCompleted(@Param("accountId") Long accountId,
                      @Param("chapterId") Long chapterId);
}
