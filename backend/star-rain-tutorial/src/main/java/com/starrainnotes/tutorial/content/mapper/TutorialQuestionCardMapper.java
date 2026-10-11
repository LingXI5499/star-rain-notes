/*
 * TutorialQuestionCardMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 阅读入口：cardIds（卡片标识集合处理入口）；deleteQuestion（删除问题）；deleteCard（删除卡片）；insert（写入教程问题卡片）。
 */

package com.starrainnotes.tutorial.content.mapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/**
 * 教程问题卡片数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper public interface TutorialQuestionCardMapper {
 /**
  * 卡片标识集合处理入口。
  *
  * 持久化操作：读取，涉及 <code>sr_tutorial_question_card</code>。
  *
  * @param questionId 问题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("questionId")</code>
  * @return 本次处理得到的条目集合
  */
 List<Long> cardIds(@Param("questionId") Long questionId);
 /**
  * 删除问题。
  *
  * 持久化操作：删除，涉及 <code>sr_tutorial_question_card</code>。
  *
  * @param questionId 问题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("questionId")</code>
  * @return SQL 写入影响的记录行数
  */
 int deleteQuestion(@Param("questionId") Long questionId);
 /**
  * 删除卡片。
  *
  * 持久化操作：删除，涉及 <code>sr_tutorial_question_card</code>。
  *
  * @param cardId 卡片标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("cardId")</code>
  * @return SQL 写入影响的记录行数
  */
 int deleteCard(@Param("cardId") Long cardId);
 /**
  * 写入教程问题卡片。
  *
  * 持久化操作：新增，涉及 <code>sr_tutorial_question_card</code>。
  *
  * @param questionId 问题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("questionId")</code>
  * @param cardId 卡片标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("cardId")</code>
  * @param sortOrder 展示排序值；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("sortOrder")</code>
  * @return SQL 写入影响的记录行数
  */
 int insert(@Param("questionId") Long questionId,@Param("cardId") Long cardId,@Param("sortOrder") int sortOrder);
}
