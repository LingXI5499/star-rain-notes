/*
 * TutorialLearningCleanupMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 阅读入口：cancelPlans（取消Plans）；skipCancelledTasks（skipCancelledTasks处理入口）；deleteChapterRelations（删除章节Relations）。
 */

package com.starrainnotes.tutorial.content.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Retires affected plans while preserving immutable personal learning history. */
@Mapper
public interface TutorialLearningCleanupMapper {
    /**
     * 取消Plans。
     *
     * 持久化操作：更新，涉及 <code>sr_study_plan</code>。
     *
     * @param scope scope；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("scope")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int cancelPlans(@Param("scope") String scope, @Param("id") Long id);
    /**
     * skipCancelledTasks处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_study_task</code>、<code>sr_study_plan</code>。
     *
     * @param scope scope；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("scope")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int skipCancelledTasks(@Param("scope") String scope, @Param("id") Long id);
    /**
     * 删除章节Relations。
     *
     * 持久化操作：删除，涉及 <code>sr_tutorial_question_card</code>、<code>sr_tutorial_question</code>、<code>sr_tutorial_knowledge_card</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("chapterId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteChapterRelations(@Param("chapterId") Long chapterId);
}
