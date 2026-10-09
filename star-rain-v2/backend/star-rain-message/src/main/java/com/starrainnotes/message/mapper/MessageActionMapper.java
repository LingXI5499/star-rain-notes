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
public interface MessageActionMapper {

    int insert(MessageActionEntity entry);

    List<MessageActionEntity> listByMessage(@Param("messageId") long messageId);
}
