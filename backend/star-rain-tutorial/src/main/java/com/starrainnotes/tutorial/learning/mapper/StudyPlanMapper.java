/*
 * StudyPlanMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：StudyPlanEntity。
 * 阅读入口：insert（写入学习计划）；selectById（按标识读取查询）；listRecentByAccountId（列出Recent依据账号标识）；countActiveByAccountId（统计有效依据账号标识）。
 */

package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.StudyPlanEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 个人学习计划的全部 SQL 都在 mapper/tutorial/StudyPlanMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 学习计划数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface StudyPlanMapper {

    /**
     * 写入学习计划。
     *
     * 持久化操作：新增，涉及 <code>sr_study_plan</code>。
     *
     * @param plan 计划；类型为 <code>StudyPlanEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(StudyPlanEntity plan);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_study_plan</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>StudyPlanEntity</code> 的处理结果
     */
    StudyPlanEntity selectById(@Param("id") Long id);

    /**
     * 列出Recent依据账号标识。
     *
     * 持久化操作：读取，涉及 <code>sr_study_plan</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<StudyPlanEntity> listRecentByAccountId(@Param("accountId") Long accountId);

    /**
     * 统计有效依据账号标识。
     *
     * 持久化操作：读取，涉及 <code>sr_study_plan</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countActiveByAccountId(@Param("accountId") Long accountId);

    /*
     * 保存计划定义。end_date / daily_target_minutes 允许为空，空值不覆盖已有值 ——
     * 这与原先 updateById 的 NOT_NULL 字段策略一致，改成一个无条件 SET 会让「不传」变成「清空」。
     */
    /**
     * 更新Definition。
     *
     * 持久化操作：更新，涉及 <code>sr_study_plan</code>。
     *
     * @param plan 计划；类型为 <code>StudyPlanEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateDefinition(StudyPlanEntity plan);

    /**
     * 更新状态。
     *
     * 持久化操作：更新，涉及 <code>sr_study_plan</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param generatedVersion generated版本；类型为 <code>Integer</code>；对应 MyBatis 命名参数 <code>@Param("generatedVersion")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("generatedVersion") Integer generatedVersion);
}
