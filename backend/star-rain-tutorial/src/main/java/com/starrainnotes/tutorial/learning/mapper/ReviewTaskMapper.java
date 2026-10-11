/*
 * ReviewTaskMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：ReviewTaskEntity。
 * 阅读入口：insertDue（写入Due）；markOverdue（markOverdue处理入口）；complete（完成审核任务）；selectById（按标识读取查询）。
 */

package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.ReviewTaskEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 复习任务的全部 SQL 都在 mapper/tutorial/ReviewTaskMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 审核任务数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface ReviewTaskMapper {

    /* 同一复习计划同一到期时间只生成一条任务，重复入队由唯一键挡住。 */
    /**
     * 写入Due。
     *
     * 持久化操作：新增，涉及 <code>sr_review_task</code>。
     *
     * @param scheduleId schedule标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("scheduleId")</code>
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("cardId")</code>
     * @param dueAt due时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("dueAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertDue(@Param("scheduleId") Long scheduleId, @Param("accountId") Long accountId,
                  @Param("cardId") Long cardId, @Param("dueAt") LocalDateTime dueAt);

    /**
     * markOverdue处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_review_task</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     * @return SQL 写入影响的记录行数
     */
    int markOverdue(@Param("accountId") Long accountId, @Param("now") LocalDateTime now);

    /**
     * 完成审核任务。
     *
     * 持久化操作：更新，涉及 <code>sr_review_task</code>。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("taskId")</code>
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int complete(@Param("taskId") Long taskId, @Param("accountId") Long accountId);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_review_task</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>ReviewTaskEntity</code> 的处理结果
     */
    ReviewTaskEntity selectById(@Param("id") Long id);

    /**
     * 列出待处理依据账号标识。
     *
     * 持久化操作：读取，涉及 <code>sr_review_task</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<ReviewTaskEntity> listPendingByAccountId(@Param("accountId") Long accountId);

    /**
     * 统计已完成依据账号标识。
     *
     * 持久化操作：读取，涉及 <code>sr_review_task</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countCompletedByAccountId(@Param("accountId") Long accountId);
}
