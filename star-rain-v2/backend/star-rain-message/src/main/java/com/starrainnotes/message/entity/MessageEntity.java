package com.starrainnotes.message.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 留言实体。列名到属性名的映射由 application.yml 的 map-underscore-to-camel-case 完成，
 * 表名由各 Mapper XML 的语句写明，因此这里不保留 MyBatis-Plus 的 @TableName / @TableId
 * （本模块的 Mapper 已不继承 BaseMapper，那两个注解不会再生效，留着只会让人误以为框架仍在拼 SQL）。
 */
@Data
public class MessageEntity {
    private Long id;
    private Long accountId;
    private String authorDisplayName;
    private String contactEmail;
    private String content;
    private String status;
    private LocalDateTime submittedAt;
    private Long moderatedByAccountId;
    private LocalDateTime moderatedAt;
    private String rejectReason;
    private LocalDateTime hiddenAt;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
