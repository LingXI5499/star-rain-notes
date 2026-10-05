package com.starrainnotes.tutorial.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.learning.entity.UserQuestionAnswerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserQuestionAnswerMapper extends BaseMapper<UserQuestionAnswerEntity> {

    int upsertAnswer(@Param("accountId") Long accountId,
                     @Param("questionId") Long questionId,
                     @Param("answerText") String answerText);
}
