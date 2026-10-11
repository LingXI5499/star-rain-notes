/*
 * EvidenceLearningMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：LearningPageQueryDTO。
 * 阅读入口：saveSessionSummary（保存会话摘要）；latestInitial（最新Initial处理入口）；ensureAccountLock（确保账号锁）；lockAccount（锁账号处理入口）。
 */

package com.starrainnotes.tutorial.learning.mapper;
import com.starrainnotes.tutorial.learning.entity.EvidenceModels.*;
import com.starrainnotes.tutorial.learning.dto.LearningPageQueryDTO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/**
 * 证据学习数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface EvidenceLearningMapper {
    /**
     * 保存会话摘要。
     *
     * 持久化操作：更新，涉及 <code>sr_learning_session</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param summaryJson 摘要JSON；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("summaryJson")</code>
     * @return SQL 写入影响的记录行数
     */
    int saveSessionSummary(@Param("accountId") Long accountId, @Param("id") Long id, @Param("summaryJson") String summaryJson);
    /**
     * 最新Initial处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_session</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("chapterId")</code>
     * @return 类型为 <code>Session</code> 的处理结果
     */
    Session latestInitial(@Param("accountId") Long accountId, @Param("chapterId") Long chapterId);
    /**
     * 确保账号锁。
     *
     * 持久化操作：新增，涉及 <code>sr_learning_account_lock</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int ensureAccountLock(@Param("accountId") Long accountId);
    /**
     * 锁账号处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_account_lock</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 类型为 <code>Long</code> 的处理结果
     */
    Long lockAccount(@Param("accountId") Long accountId);
    /**
     * 列出Plans。
     *
     * 持久化操作：读取，涉及 <code>sr_study_plan</code>、<code>sr_tutorial</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Plan> listPlans(@Param("accountId") Long accountId);
    /**
     * 当前Plans处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_study_plan</code>、<code>sr_tutorial</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Plan> currentPlans(@Param("accountId") Long accountId);
    /**
     * 计划历史处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_study_plan_chapter</code>、<code>sr_study_plan</code>、<code>sr_tutorial</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param query 查询参数；类型为 <code>LearningPageQueryDTO</code>；对应 MyBatis 命名参数 <code>@Param("query")</code>
     * @return 符合 SQL 条件的记录数量
     */
    List<Plan> planHistory(@Param("accountId") Long accountId, @Param("query") LearningPageQueryDTO query);
    /**
     * 统计计划历史。
     *
     * 持久化操作：读取，涉及 <code>sr_study_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param query 查询参数；类型为 <code>LearningPageQueryDTO</code>；对应 MyBatis 命名参数 <code>@Param("query")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countPlanHistory(@Param("accountId") Long accountId, @Param("query") LearningPageQueryDTO query);
    /**
     * 计划处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_study_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>Plan</code> 的处理结果
     */
    Plan plan(@Param("accountId") Long accountId, @Param("id") Long id);
    /**
     * restart计划处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_study_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int restartPlan(@Param("accountId") Long accountId, @Param("id") Long id);
    /**
     * 清理计划完成。
     *
     * 持久化操作：更新，涉及 <code>sr_study_plan_chapter</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int clearPlanCompletion(@Param("accountId") Long accountId, @Param("id") Long id);
    /**
     * 完成计划章节。
     *
     * 持久化操作：更新，涉及 <code>sr_study_plan_chapter</code>、<code>sr_study_plan</code>、<code>sr_learning_session</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param sessionId 会话标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("sessionId")</code>
     * @return SQL 写入影响的记录行数
     */
    int completePlanChapter(@Param("accountId") Long accountId, @Param("sessionId") Long sessionId);
    /**
     * 计划会话处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_session</code>、<code>sr_study_plan</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("chapterId")</code>
     * @param planId 计划标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("planId")</code>
     * @return 类型为 <code>Session</code> 的处理结果
     */
    Session planSession(@Param("accountId") Long accountId, @Param("chapterId") Long chapterId, @Param("planId") Long planId);
    /**
     * 打开InitialSessions。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_session</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("chapterId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Session> openInitialSessions(@Param("accountId") Long accountId, @Param("chapterId") Long chapterId);
    /**
     * 写入会话问题。
     *
     * 持久化操作：新增，涉及 <code>sr_learning_session_question</code>。
     *
     * @param sessionId 会话标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("sessionId")</code>
     * @param questionId 问题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("questionId")</code>
     * @param baselineVersion baseline版本；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("baselineVersion")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertSessionQuestion(@Param("sessionId") Long sessionId, @Param("questionId") Long questionId, @Param("baselineVersion") int baselineVersion);
    /**
     * answered面向会话处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_session_question</code>、<code>sr_learning_session</code>、<code>sr_user_question_answer</code>、<code>sr_user_question_answer_version</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param sessionId 会话标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("sessionId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Long> answeredForSession(@Param("accountId") Long accountId, @Param("sessionId") Long sessionId);
    /**
     * 写入计划。
     *
     * 持久化操作：新增，涉及 <code>sr_study_plan</code>。
     *
     * @param row 行；类型为 <code>Plan</code>；对应 MyBatis 命名参数 <code>@Param("row")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertPlan(@Param("row") Plan row);
    /**
     * 更新计划。
     *
     * 持久化操作：更新，涉及 <code>sr_study_plan</code>。
     *
     * @param row 行；类型为 <code>Plan</code>；对应 MyBatis 命名参数 <code>@Param("row")</code>
     * @return SQL 写入影响的记录行数
     */
    int updatePlan(@Param("row") Plan row);
    /**
     * 计划状态处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_study_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return SQL 写入影响的记录行数
     */
    int planStatus(@Param("accountId") Long accountId, @Param("id") Long id, @Param("status") String status);
    /**
     * 计划章节列表处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_study_plan_chapter</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param planId 计划标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("planId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Chapter> planChapters(@Param("accountId") Long accountId, @Param("planId") Long planId);
    /**
     * conflicting章节列表处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_study_plan_chapter</code>、<code>sr_study_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param excludePlanId exclude计划标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("excludePlanId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Long> conflictingChapters(@Param("accountId") Long accountId, @Param("excludePlanId") Long excludePlanId);
    /**
     * 写入计划章节。
     *
     * 持久化操作：新增，涉及 <code>sr_study_plan_chapter</code>。
     *
     * @param row 行；类型为 <code>Chapter</code>；对应 MyBatis 命名参数 <code>@Param("row")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertPlanChapter(@Param("row") Chapter row);
    /**
     * 删除计划章节列表。
     *
     * 持久化操作：删除，涉及 <code>sr_study_plan_chapter</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param planId 计划标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("planId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deletePlanChapters(@Param("accountId") Long accountId, @Param("planId") Long planId);
    /**
     * 写入任务。
     *
     * 持久化操作：新增，涉及 <code>sr_study_task</code>。
     *
     * @param row 行；类型为 <code>Task</code>；对应 MyBatis 命名参数 <code>@Param("row")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertTask(@Param("row") Task row);
    /**
     * 写入任务章节。
     *
     * 持久化操作：新增，涉及 <code>sr_study_task_chapter</code>。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("taskId")</code>
     * @param row 行；类型为 <code>Chapter</code>；对应 MyBatis 命名参数 <code>@Param("row")</code>
     * @param sequenceNo sequence不含；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("sequenceNo")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertTaskChapter(@Param("taskId") Long taskId, @Param("row") Chapter row, @Param("sequenceNo") int sequenceNo);
    /**
     * tasks处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_study_task</code>、<code>sr_study_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param planId 计划标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("planId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Task> tasks(@Param("accountId") Long accountId, @Param("planId") Long planId);
    /**
     * 任务处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_study_task</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>Task</code> 的处理结果
     */
    Task task(@Param("accountId") Long accountId, @Param("id") Long id);
    /**
     * 任务章节列表处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_study_task_chapter</code>、<code>sr_study_task</code>、<code>sr_study_plan_chapter</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param taskId 任务标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("taskId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Chapter> taskChapters(@Param("accountId") Long accountId, @Param("taskId") Long taskId);
    /**
     * 删除任务章节列表。
     *
     * 持久化操作：删除，涉及 <code>sr_study_task_chapter</code>、<code>sr_study_task</code>、<code>sr_study_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param planId 计划标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("planId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteTaskChapters(@Param("accountId") Long accountId, @Param("planId") Long planId);
    /**
     * 删除Tasks。
     *
     * 持久化操作：删除，涉及 <code>sr_study_task</code>、<code>sr_study_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param planId 计划标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("planId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteTasks(@Param("accountId") Long accountId, @Param("planId") Long planId);
    /**
     * 开始任务处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_study_task</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int startTask(@Param("accountId") Long accountId, @Param("id") Long id);
    /**
     * reconcileTasks处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_study_task</code>、<code>sr_study_task_chapter</code>、<code>sr_study_plan_chapter</code>、<code>sr_study_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int reconcileTasks(@Param("accountId") Long accountId);
    /**
     * reconcilePlans处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_study_plan</code>、<code>sr_study_task</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int reconcilePlans(@Param("accountId") Long accountId);
    /**
     * 当前业务使用的会话对象处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_session</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>Session</code> 的处理结果
     */
    Session session(@Param("accountId") Long accountId, @Param("id") Long id);
    /**
     * 当前Initial处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_session</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("chapterId")</code>
     * @return 类型为 <code>Session</code> 的处理结果
     */
    Session currentInitial(@Param("accountId") Long accountId, @Param("chapterId") Long chapterId);
    /**
     * 当前审核处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_session</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 类型为 <code>Session</code> 的处理结果
     */
    Session currentReview(@Param("accountId") Long accountId);
    /**
     * 写入会话。
     *
     * 持久化操作：新增，涉及 <code>sr_learning_session</code>。
     *
     * @param row 行；类型为 <code>Session</code>；对应 MyBatis 命名参数 <code>@Param("row")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertSession(@Param("row") Session row);
    /**
     * 会话状态处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_learning_session</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return SQL 写入影响的记录行数
     */
    int sessionStatus(@Param("accountId") Long accountId, @Param("id") Long id, @Param("status") String status);
    /**
     * 条目集合处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_session_item</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param sessionId 会话标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("sessionId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Item> items(@Param("accountId") Long accountId, @Param("sessionId") Long sessionId);
    /**
     * 写入Item。
     *
     * 持久化操作：新增，涉及 <code>sr_learning_session_item</code>。
     *
     * @param row 行；类型为 <code>Item</code>；对应 MyBatis 命名参数 <code>@Param("row")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertItem(@Param("row") Item row);
    /**
     * revealItem处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_learning_session_item</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param sessionId 会话标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("sessionId")</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("cardId")</code>
     * @return SQL 写入影响的记录行数
     */
    int revealItem(@Param("accountId") Long accountId, @Param("sessionId") Long sessionId, @Param("cardId") Long cardId);
    /**
     * 完成Item。
     *
     * 持久化操作：更新，涉及 <code>sr_learning_session_item</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param sessionId 会话标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("sessionId")</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("cardId")</code>
     * @return SQL 写入影响的记录行数
     */
    int completeItem(@Param("accountId") Long accountId, @Param("sessionId") Long sessionId, @Param("cardId") Long cardId);
    /**
     * initial卡片标识集合处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_evidence</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("chapterId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Long> initialCardIds(@Param("accountId") Long accountId, @Param("chapterId") Long chapterId);
    /**
     * 写入证据。
     *
     * 持久化操作：新增，涉及 <code>sr_learning_evidence</code>。
     *
     * @param row 行；类型为 <code>Evidence</code>；对应 MyBatis 命名参数 <code>@Param("row")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertEvidence(@Param("row") Evidence row);
    /**
     * 卡片证据处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_evidence</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("cardId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Evidence> cardEvidence(@Param("accountId") Long accountId, @Param("cardId") Long cardId);
    /**
     * 会话证据处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_evidence</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param sessionId 会话标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("sessionId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Evidence> sessionEvidence(@Param("accountId") Long accountId, @Param("sessionId") Long sessionId);
    /**
     * masteries处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_knowledge_mastery</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Mastery> masteries(@Param("accountId") Long accountId);
    /**
     * 新增或更新掌握。
     *
     * 持久化操作：新增，涉及 <code>sr_knowledge_mastery</code>。
     *
     * @param row 行；类型为 <code>Mastery</code>；对应 MyBatis 命名参数 <code>@Param("row")</code>
     * @return SQL 写入影响的记录行数
     */
    int upsertMastery(@Param("row") Mastery row);
    /**
     * 答案处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_user_question_answer</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param questionId 问题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("questionId")</code>
     * @return 类型为 <code>Answer</code> 的处理结果
     */
    Answer answer(@Param("accountId") Long accountId, @Param("questionId") Long questionId);
    /**
     * 写入答案。
     *
     * 持久化操作：新增，涉及 <code>sr_user_question_answer</code>。
     *
     * @param row 行；类型为 <code>Answer</code>；对应 MyBatis 命名参数 <code>@Param("row")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertAnswer(@Param("row") Answer row);
    /**
     * 写入答案版本。
     *
     * 持久化操作：新增，涉及 <code>sr_user_question_answer_version</code>。
     *
     * @param row 行；类型为 <code>AnswerVersion</code>；对应 MyBatis 命名参数 <code>@Param("row")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertAnswerVersion(@Param("row") AnswerVersion row);
    /**
     * 答案Versions处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_user_question_answer_version</code>、<code>sr_user_question_answer</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param questionId 问题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("questionId")</code>
     * @return 本次处理得到的条目集合
     */
    List<AnswerVersion> answerVersions(@Param("accountId") Long accountId, @Param("questionId") Long questionId);
    /**
     * 更新答案响应头。
     *
     * 持久化操作：更新，涉及 <code>sr_user_question_answer</code>。
     *
     * @param row 行；类型为 <code>Answer</code>；对应 MyBatis 命名参数 <code>@Param("row")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateAnswerHeader(@Param("row") Answer row);
    /**
     * answered问题标识集合处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_user_question_answer</code>、<code>sr_user_question_answer_version</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Long> answeredQuestionIds(@Param("accountId") Long accountId);
    /**
     * 历史Sessions处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_session</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<Session> historySessions(@Param("accountId") Long accountId, @Param("offset") int offset, @Param("limit") int limit);
    /**
     * 统计历史Sessions。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_session</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countHistorySessions(@Param("accountId") Long accountId);
    /**
     * filtered历史处理入口。
     *
     * 持久化语句类型：<code>select</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param query 查询参数；类型为 <code>LearningPageQueryDTO</code>；对应 MyBatis 命名参数 <code>@Param("query")</code>
     * @return 本次处理得到的条目集合
     */
    List<Session> filteredHistory(@Param("accountId") Long accountId, @Param("query") LearningPageQueryDTO query);
    /**
     * 统计Filtered历史。
     *
     * 持久化语句类型：<code>select</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param query 查询参数；类型为 <code>LearningPageQueryDTO</code>；对应 MyBatis 命名参数 <code>@Param("query")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countFilteredHistory(@Param("accountId") Long accountId, @Param("query") LearningPageQueryDTO query);
    /**
     * 历史Tutorials处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>、<code>sr_study_plan</code>、<code>sr_learning_session</code>、<code>sr_learning_session_item</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<TutorialOption> historyTutorials(@Param("accountId") Long accountId);
}
