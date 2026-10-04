package com.starrainnotes.tutorial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.entity.UserQuestionAnswerEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserQuestionAnswerMapper extends BaseMapper<UserQuestionAnswerEntity> {
    @Insert("""
            INSERT INTO sr_user_question_answer
                (account_id, question_id, answer_text)
            VALUES (#{accountId}, #{questionId}, #{answerText})
            ON DUPLICATE KEY UPDATE answer_text = VALUES(answer_text)
            """)
    int upsertAnswer(@Param("accountId") Long accountId,
                     @Param("questionId") Long questionId,
                     @Param("answerText") String answerText);
}
