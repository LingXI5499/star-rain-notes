/*
 * MessageActionMapper：留言模块的数据访问文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：MessageActionEntity。
 * 阅读入口：insert（写入留言动作）；listByMessage（列出依据留言）。
 */

package com.starrainnotes.message.mapper;

import com.starrainnotes.message.entity.MessageActionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 留言操作流水。SQL 全部在 mapper/message/MessageActionMapper.xml。
 *
 * 与 MessageMapper 同样不继承 BaseMapper：这张表只有「追加一条流水」和
 * 「按留言查流水」两个访问形状，显式声明比让框架生成整套 CRUD 更能说明它只增不改。
 */
/**
 * 留言动作数据映射，属于留言模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface MessageActionMapper {

    /**
     * 写入留言动作。
     *
     * 持久化操作：新增，涉及 <code>sr_message_action</code>。
     *
     * @param entry 入口；类型为 <code>MessageActionEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(MessageActionEntity entry);

    /**
     * 列出依据留言。
     *
     * 持久化操作：读取，涉及 <code>sr_message_action</code>。
     *
     * @param messageId 留言标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("messageId")</code>
     * @return 本次处理得到的条目集合
     */
    List<MessageActionEntity> listByMessage(@Param("messageId") long messageId);
}
