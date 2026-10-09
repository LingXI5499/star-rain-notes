package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.LearningHistoryEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 个人学习事件历史的全部 SQL 都在 mapper/tutorial/LearningHistoryMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface LearningHistoryMapper {

    int insert(LearningHistoryEntity history);

    long countByAccountId(@Param("accountId") Long accountId);

    List<LearningHistoryEntity> pageByAccountId(@Param("accountId") Long accountId,
                                                @Param("limit") int limit,
                                                @Param("offset") long offset);
}
