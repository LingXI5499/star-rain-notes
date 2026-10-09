package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.UserQuestionAnswerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 用户章节问题答案的全部 SQL 都在 mapper/tutorial/UserQuestionAnswerMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface UserQuestionAnswerMapper {

    /* 同一账户同一题目只有一条答案：重复提交覆盖 answer_text，首次提交时间保持不变。 */
    int upsertAnswer(@Param("accountId") Long accountId,
                     @Param("questionId") Long questionId,
                     @Param("answerText") String answerText);

    UserQuestionAnswerEntity selectByAccountIdAndQuestionId(@Param("accountId") Long accountId,
                                                            @Param("questionId") Long questionId);
}
