package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.LearningProgressEntity;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 章节学习进度的全部 SQL 都在 mapper/tutorial/LearningProgressMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface LearningProgressMapper {

    /* 同一账户同一章节只有一行：读到哪写到哪，学习时长累加。 */
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

    /* 只在第一次完成时返回 1，用于「完成事件只写一次」的幂等判断。 */
    int markCompleted(@Param("accountId") Long accountId,
                      @Param("chapterId") Long chapterId);

    LearningProgressEntity selectByAccountIdAndChapterId(@Param("accountId") Long accountId,
                                                        @Param("chapterId") Long chapterId);

    List<LearningProgressEntity> listByAccountId(@Param("accountId") Long accountId);

    List<LearningProgressEntity> listByAccountIdAndTutorialId(@Param("accountId") Long accountId,
                                                              @Param("tutorialId") Long tutorialId);

    /* 继续阅读入口：按最近学习时间倒序取固定条数。 */
    List<LearningProgressEntity> listRecentByAccountId(@Param("accountId") Long accountId);
}
