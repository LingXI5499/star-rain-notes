/*
 * LearningHistoryMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：LearningHistoryEntity。
 * 阅读入口：insert（写入学习历史）；countByAccountId（统计依据账号标识）；pageByAccountId（分页读取依据账号标识）。
 */

package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.LearningHistoryEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 个人学习事件历史的全部 SQL 都在 mapper/tutorial/LearningHistoryMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 学习历史数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface LearningHistoryMapper {

    /**
     * 写入学习历史。
     *
     * 持久化操作：新增，涉及 <code>sr_learning_history</code>。
     *
     * @param history 历史；类型为 <code>LearningHistoryEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(LearningHistoryEntity history);

    /**
     * 统计依据账号标识。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_history</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByAccountId(@Param("accountId") Long accountId);

    /**
     * 分页读取依据账号标识。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_history</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @param offset 查询跳过的记录数；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @return 本次处理得到的条目集合
     */
    List<LearningHistoryEntity> pageByAccountId(@Param("accountId") Long accountId,
                                                @Param("limit") int limit,
                                                @Param("offset") long offset);
}
