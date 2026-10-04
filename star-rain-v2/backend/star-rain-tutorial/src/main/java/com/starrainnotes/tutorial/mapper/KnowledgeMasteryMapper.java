package com.starrainnotes.tutorial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.entity.KnowledgeMasteryEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KnowledgeMasteryMapper extends BaseMapper<KnowledgeMasteryEntity> {
    @Insert("""
            INSERT IGNORE INTO sr_knowledge_mastery
                (account_id, knowledge_card_id, system_suggested_level, evidence_count)
            VALUES (#{accountId}, #{cardId}, 'L1', 0)
            """)
    int ensure(@Param("accountId") Long accountId, @Param("cardId") Long cardId);

    @Insert("""
            INSERT IGNORE INTO sr_knowledge_mastery
                (account_id, knowledge_card_id, system_suggested_level, evidence_count, first_learned_at)
            VALUES (#{accountId}, #{cardId}, 'L1', 0, UTC_TIMESTAMP(3))
            """)
    int ensureLearned(@Param("accountId") Long accountId, @Param("cardId") Long cardId);

    @Update("""
            UPDATE sr_knowledge_mastery
            SET first_learned_at = COALESCE(first_learned_at, UTC_TIMESTAMP(3))
            WHERE account_id = #{accountId} AND knowledge_card_id = #{cardId}
            """)
    int markLearned(@Param("accountId") Long accountId, @Param("cardId") Long cardId);
}
