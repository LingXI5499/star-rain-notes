package com.starrainnotes.message.entity;

import java.time.LocalDateTime;
import lombok.Data;

/* 留言操作流水实体；表名与列名由 MessageActionMapper.xml 的语句写明，无需框架注解。 */
@Data
public class MessageActionEntity {
    private Long id;
    private Long messageId;
    private String actionType;
    private Long actorAccountId;
    private String actorType;
    private String note;
    private LocalDateTime createdAt;
}
