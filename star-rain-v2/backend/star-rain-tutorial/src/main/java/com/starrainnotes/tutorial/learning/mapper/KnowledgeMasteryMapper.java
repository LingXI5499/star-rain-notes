package com.starrainnotes.tutorial.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.learning.entity.KnowledgeMasteryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgeMasteryMapper extends BaseMapper<KnowledgeMasteryEntity> {

    int ensure(@Param("accountId") Long accountId, @Param("cardId") Long cardId);

    int ensureLearned(@Param("accountId") Long accountId, @Param("cardId") Long cardId);

    int markLearned(@Param("accountId") Long accountId, @Param("cardId") Long cardId);
}
