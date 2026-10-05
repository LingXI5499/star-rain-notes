package com.starrainnotes.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.message.entity.MessageEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;

public interface MessageMapper extends BaseMapper<MessageEntity> {

    int decide(@Param("id") long id, @Param("target") String target, @Param("actorId") long actorId,
               @Param("at") LocalDateTime at, @Param("reason") String reason);

    int hide(@Param("id") long id, @Param("at") LocalDateTime at);

    int restore(@Param("id") long id);

    int softDelete(@Param("id") long id, @Param("at") LocalDateTime at);
}
