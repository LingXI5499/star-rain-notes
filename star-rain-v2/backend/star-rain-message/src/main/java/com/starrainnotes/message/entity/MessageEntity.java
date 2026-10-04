package com.starrainnotes.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_message")
public class MessageEntity {
    @TableId(type = IdType.AUTO)
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
