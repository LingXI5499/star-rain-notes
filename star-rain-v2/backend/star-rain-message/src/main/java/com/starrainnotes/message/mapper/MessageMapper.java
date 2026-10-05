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
public interface MessageMapper {

    int insert(MessageEntity message);

    MessageEntity selectById(@Param("id") long id);

    long countByStatus(@Param("status") String status);

    List<MessageEntity> pageByStatus(@Param("status") String status,
                                     @Param("limit") int limit,
                                     @Param("offset") long offset);

    int decide(@Param("id") long id, @Param("target") String target, @Param("actorId") long actorId,
               @Param("at") LocalDateTime at, @Param("reason") String reason);

    int hide(@Param("id") long id, @Param("at") LocalDateTime at);

    int restore(@Param("id") long id);

    int softDelete(@Param("id") long id, @Param("at") LocalDateTime at);
}
