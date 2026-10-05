package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.KnowledgeMasteryEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 知识掌握状态的全部 SQL 都在 mapper/tutorial/KnowledgeMasteryMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface KnowledgeMasteryMapper {

    int ensure(@Param("accountId") Long accountId, @Param("cardId") Long cardId);

    /* 首次学习时间只在第一次写入，重复调用不会覆盖。 */
    int ensureLearned(@Param("accountId") Long accountId, @Param("cardId") Long cardId);

    int markLearned(@Param("accountId") Long accountId, @Param("cardId") Long cardId);

    KnowledgeMasteryEntity selectByAccountIdAndCardId(@Param("accountId") Long accountId,
                                                      @Param("knowledgeCardId") Long knowledgeCardId);

    List<KnowledgeMasteryEntity> listByAccountId(@Param("accountId") Long accountId);

    /* 复习完成后回写证据统计与系统建议等级。 */
    int updateEvidence(@Param("id") Long id, @Param("evidenceCount") int evidenceCount,
                       @Param("lastRecallRating") String lastRecallRating,
                       @Param("lastEvidenceAt") LocalDateTime lastEvidenceAt,
                       @Param("systemSuggestedLevel") String systemSuggestedLevel);

    int updateSelfLevel(@Param("id") Long id, @Param("userSelfLevel") String userSelfLevel);
}
