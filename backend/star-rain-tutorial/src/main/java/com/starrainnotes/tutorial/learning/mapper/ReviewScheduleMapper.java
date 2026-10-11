/*
 * ReviewScheduleMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：ReviewScheduleEntity。
 * 阅读入口：ensure（确保审核Schedule）；dueUnqueued（dueUnqueued处理入口）；dueUnqueuedForAccount（dueUnqueued面向账号处理入口）；selectById（按标识读取查询）。
 */

package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.ReviewScheduleEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 复习计划的全部 SQL 都在 mapper/tutorial/ReviewScheduleMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 审核Schedule数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface ReviewScheduleMapper {

    /* 同一账户同一卡片只有一条计划，首次学习时建立。 */
    /**
     * 确保审核Schedule。
     *
     * 持久化操作：新增，涉及 <code>sr_review_schedule</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("cardId")</code>
     * @param days days；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("days")</code>
     * @param nextAt 下一项时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("nextAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int ensure(@Param("accountId") Long accountId, @Param("cardId") Long cardId,
               @Param("days") int days, @Param("nextAt") LocalDateTime nextAt);

    /**
     * dueUnqueued处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_review_schedule</code>、<code>sr_review_task</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     * @return 本次处理得到的条目集合
     */
    List<ReviewScheduleEntity> dueUnqueued(@Param("now") LocalDateTime now);

    /**
     * dueUnqueued面向账号处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_review_schedule</code>、<code>sr_review_task</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     * @return 本次处理得到的条目集合
     */
    List<ReviewScheduleEntity> dueUnqueuedForAccount(
            @Param("accountId") Long accountId, @Param("now") LocalDateTime now);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_review_schedule</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>ReviewScheduleEntity</code> 的处理结果
     */
    ReviewScheduleEntity selectById(@Param("id") Long id);

    /* 复习完成后推进步骤、间隔与下次复习时间。 */
    /**
     * 更新之后审核。
     *
     * 持久化操作：更新，涉及 <code>sr_review_schedule</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param stepIndex step索引；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("stepIndex")</code>
     * @param currentIntervalDays 当前间隔Days；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("currentIntervalDays")</code>
     * @param nextReviewAt 下一次计划复习时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("nextReviewAt")</code>
     * @param lastResultAt 最近结果时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("lastResultAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateAfterReview(@Param("id") Long id, @Param("stepIndex") int stepIndex,
                          @Param("currentIntervalDays") int currentIntervalDays,
                          @Param("nextReviewAt") LocalDateTime nextReviewAt,
                          @Param("lastResultAt") LocalDateTime lastResultAt);
}
