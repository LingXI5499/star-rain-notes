package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.ReviewResultEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 单次复习结果的全部 SQL 都在 mapper/tutorial/ReviewResultMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface ReviewResultMapper {

    int insert(ReviewResultEntity result);

    /* 系统建议等级依据：这张卡片历史上「正常/轻松」的回忆次数。 */
    long countGoodByAccountIdAndCardId(@Param("accountId") Long accountId,
                                       @Param("knowledgeCardId") Long knowledgeCardId);
}
