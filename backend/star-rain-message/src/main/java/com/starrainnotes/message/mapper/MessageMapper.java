/*
 * MessageMapper：留言模块的数据访问文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：MessageEntity。
 * 阅读入口：insert（写入留言）；selectById（按标识读取查询）；countByStatus（统计依据状态）；pageByStatus（分页读取依据状态）。
 */

package com.starrainnotes.message.mapper;

import com.starrainnotes.message.entity.MessageEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 留言表的全部 SQL 都在 mapper/message/MessageMapper.xml。
 *
 * 本接口不再继承 MyBatis-Plus 的 BaseMapper：那样虽然写起来省事，但 selectById / insert /
 * count / 分页这些语句由框架在运行时拼，源码里看不到实际 SQL，
 * 既不符合「所有实际 SQL 写在 XML」的项目约定，也无法做参数与结果映射的静态核对。
 * 逐个方法显式声明后，每个方法都能在 XML 里找到唯一对应的一条语句。
 */
/**
 * 留言数据映射，属于留言模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface MessageMapper {

    /**
     * 写入留言。
     *
     * 持久化操作：新增，涉及 <code>sr_message</code>。
     *
     * @param message 留言；类型为 <code>MessageEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(MessageEntity message);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_message</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>MessageEntity</code> 的处理结果
     */
    MessageEntity selectById(@Param("id") long id);

    /**
     * 统计依据状态。
     *
     * 持久化操作：读取，涉及 <code>sr_message</code>。
     *
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByStatus(@Param("status") String status);

    /**
     * 分页读取依据状态。
     *
     * 持久化操作：读取，涉及 <code>sr_message</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @param offset 查询跳过的记录数；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @return 本次处理得到的条目集合
     */
    List<MessageEntity> pageByStatus(@Param("status") String status,
                                     @Param("limit") int limit,
                                     @Param("offset") long offset);

    /**
     * 作出决定留言。
     *
     * 持久化操作：更新，涉及 <code>sr_message</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param target 目标；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("target")</code>
     * @param actorId 操作者标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("actorId")</code>
     * @param at 时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("at")</code>
     * @param reason 原因；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("reason")</code>
     * @return SQL 写入影响的记录行数
     */
    int decide(@Param("id") long id, @Param("target") String target, @Param("actorId") long actorId,
               @Param("at") LocalDateTime at, @Param("reason") String reason);

    /**
     * hide处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_message</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param at 时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("at")</code>
     * @return SQL 写入影响的记录行数
     */
    int hide(@Param("id") long id, @Param("at") LocalDateTime at);

    /**
     * 恢复留言。
     *
     * 持久化操作：更新，涉及 <code>sr_message</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int restore(@Param("id") long id);

    /**
     * soft删除处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_message</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param at 时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("at")</code>
     * @return SQL 写入影响的记录行数
     */
    int softDelete(@Param("id") long id, @Param("at") LocalDateTime at);
}
