/*
 * StudyTaskMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：StudyTaskEntity。
 * 阅读入口：insertPlanned（写入Planned）；markOverdue（markOverdue处理入口）；start（开始处理入口）；complete（完成学习任务）。
 */

package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.StudyTaskEntity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 学习任务的全部 SQL 都在 mapper/tutorial/StudyTaskMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 学习任务数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface StudyTaskMapper {

    /* 计划重算时按唯一键幂等补行，重复生成不会产生第二条任务。 */
    /**
     * 写入Planned。
     *
     * 持久化操作：新增，涉及 <code>sr_study_task</code>。
     *
     * @param planId 计划标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("planId")</code>
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param tutorialId 教程标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tutorialId")</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("chapterId")</code>
     * @param taskDate 任务日期；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("taskDate")</code>
     * @param sequenceNo sequence不含；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("sequenceNo")</code>
     * @param version 版本；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("version")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertPlanned(@Param("planId") Long planId, @Param("accountId") Long accountId,
                      @Param("tutorialId") Long tutorialId, @Param("chapterId") Long chapterId,
                      @Param("taskDate") LocalDate taskDate, @Param("sequenceNo") int sequenceNo,
                      @Param("version") int version);

    /**
     * markOverdue处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_study_task</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param today today；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("today")</code>
     * @return SQL 写入影响的记录行数
     */
    int markOverdue(@Param("accountId") Long accountId, @Param("today") LocalDate today);

    /**
     * 开始处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_study_task</code>。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("taskId")</code>
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int start(@Param("taskId") Long taskId, @Param("accountId") Long accountId);

    /**
     * 完成学习任务。
     *
     * 持久化操作：更新，涉及 <code>sr_study_task</code>。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("taskId")</code>
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int complete(@Param("taskId") Long taskId, @Param("accountId") Long accountId);

    /**
     * skip处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_study_task</code>。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("taskId")</code>
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int skip(@Param("taskId") Long taskId, @Param("accountId") Long accountId);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_study_task</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>StudyTaskEntity</code> 的处理结果
     */
    StudyTaskEntity selectById(@Param("id") Long id);

    /**
     * 列出依据计划标识。
     *
     * 持久化操作：读取，涉及 <code>sr_study_task</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param planId 计划标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("planId")</code>
     * @return 本次处理得到的条目集合
     */
    List<StudyTaskEntity> listByPlanId(@Param("planId") Long planId);

    /**
     * 列出已完成依据计划标识。
     *
     * 持久化操作：读取，涉及 <code>sr_study_task</code>。
     *
     * @param planId 计划标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("planId")</code>
     * @return 本次处理得到的条目集合
     */
    List<StudyTaskEntity> listCompletedByPlanId(@Param("planId") Long planId);

    /**
     * 列出Due依据账号标识。
     *
     * 持久化操作：读取，涉及 <code>sr_study_task</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param today today；类型为 <code>LocalDate</code>；对应 MyBatis 命名参数 <code>@Param("today")</code>
     * @return 本次处理得到的条目集合
     */
    List<StudyTaskEntity> listDueByAccountId(@Param("accountId") Long accountId,
                                             @Param("today") LocalDate today);

    /* 计划重算：未完成的任务整体作废，已完成的任务保留。 */
    /**
     * 删除Unfinished依据计划标识。
     *
     * 持久化操作：删除，涉及 <code>sr_study_task</code>。
     *
     * @param planId 计划标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("planId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteUnfinishedByPlanId(@Param("planId") Long planId);

    /* 结束计划：把未完成任务标记为跳过。 */
    /**
     * 按计划标识读取skipUnfinished。
     *
     * 持久化操作：更新，涉及 <code>sr_study_task</code>。
     *
     * @param planId 计划标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("planId")</code>
     * @return SQL 写入影响的记录行数
     */
    int skipUnfinishedByPlanId(@Param("planId") Long planId);
}
