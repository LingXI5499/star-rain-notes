/*
 * TutorialOperationMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：TutorialOperationEntity。
 * 阅读入口：recordFailure（记录失败）；lockCatalog（锁Catalog处理入口）；insert（写入教程操作）；byIdForUpdate（依据标识面向更新处理入口）。
 */

package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialOperationEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 教程操作数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface TutorialOperationMapper {
    /**
     * 记录失败。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_operation</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param message 留言；类型为 <code>String</code>
     * @return SQL 写入影响的记录行数
     */
    int recordFailure(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("message") String message);
    /**
     * 锁Catalog处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_category</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     * @return 本次处理得到的条目集合
     */
    List<Long> lockCatalog();
    /**
     * 写入教程操作。
     *
     * 持久化操作：新增，涉及 <code>sr_tutorial_operation</code>。
     *
     * @param row 行；类型为 <code>TutorialOperationEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(TutorialOperationEntity row);
    /**
     * 依据标识面向更新处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_operation</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>TutorialOperationEntity</code> 的处理结果
     */
    TutorialOperationEntity byIdForUpdate(@Param("id") Long id);
    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_operation</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>TutorialOperationEntity</code> 的处理结果
     */
    TutorialOperationEntity selectById(@Param("id") Long id);
    /**
     * 待处理处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_operation</code>。
     *
     * @param scope scope；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("scope")</code>
     * @return 类型为 <code>TutorialOperationEntity</code> 的处理结果
     */
    TutorialOperationEntity pending(@Param("scope") String scope);
    /**
     * 待处理Operations处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_operation</code>。
     * @return 本次处理得到的条目集合
     */
    List<TutorialOperationEntity> pendingOperations();
    /**
     * finish处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_operation</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return SQL 写入影响的记录行数
     */
    int finish(@Param("id") Long id, @Param("status") String status);
}
