package com.starrainnotes.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_message_action")
public class MessageActionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long messageId;
    private String actionType;
    private Long actorAccountId;
    private String actorType;
    private String note;
    private LocalDateTime createdAt;
}
