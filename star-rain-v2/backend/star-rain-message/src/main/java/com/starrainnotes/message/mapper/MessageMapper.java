package com.starrainnotes.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.message.entity.MessageEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface MessageMapper extends BaseMapper<MessageEntity> {
    @Update("UPDATE sr_message SET status=#{target}, moderated_by_account_id=#{actorId}, "
            + "moderated_at=#{at}, reject_reason=#{reason} WHERE id=#{id} AND status='PENDING'")
    int decide(@Param("id") long id, @Param("target") String target, @Param("actorId") long actorId,
               @Param("at") LocalDateTime at, @Param("reason") String reason);

    @Update("UPDATE sr_message SET status='HIDDEN', hidden_at=#{at} WHERE id=#{id} AND status='PUBLIC'")
    int hide(@Param("id") long id, @Param("at") LocalDateTime at);

    @Update("UPDATE sr_message SET status='PUBLIC', hidden_at=NULL WHERE id=#{id} AND status='HIDDEN'")
    int restore(@Param("id") long id);

    @Update("UPDATE sr_message SET status='DELETED', deleted_at=#{at} "
            + "WHERE id=#{id} AND status IN ('PUBLIC','HIDDEN')")
    int softDelete(@Param("id") long id, @Param("at") LocalDateTime at);
}
