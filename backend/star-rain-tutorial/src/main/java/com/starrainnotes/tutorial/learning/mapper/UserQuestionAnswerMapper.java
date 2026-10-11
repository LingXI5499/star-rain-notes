/*
 * UserQuestionAnswerMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：UserQuestionAnswerEntity。
 * 阅读入口：upsertAnswer（新增或更新答案）；selectByAccountIdAndQuestionId（按账号标识及问题标识读取查询）。
 */

package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.UserQuestionAnswerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 用户章节问题答案的全部 SQL 都在 mapper/tutorial/UserQuestionAnswerMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 用户问题答案数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface UserQuestionAnswerMapper {

    /* 同一账户同一题目只有一条答案：重复提交覆盖 answer_text，首次提交时间保持不变。 */
    /**
     * 新增或更新答案。
     *
     * 持久化操作：新增，涉及 <code>sr_user_question_answer</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param questionId 问题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("questionId")</code>
     * @param answerText 用户提交的回答文本；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("answerText")</code>
     * @return SQL 写入影响的记录行数
     */
    int upsertAnswer(@Param("accountId") Long accountId,
                     @Param("questionId") Long questionId,
                     @Param("answerText") String answerText);

    /**
     * 按账号标识及问题标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_user_question_answer</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param questionId 问题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("questionId")</code>
     * @return 类型为 <code>UserQuestionAnswerEntity</code> 的处理结果
     */
    UserQuestionAnswerEntity selectByAccountIdAndQuestionId(@Param("accountId") Long accountId,
                                                            @Param("questionId") Long questionId);
}
