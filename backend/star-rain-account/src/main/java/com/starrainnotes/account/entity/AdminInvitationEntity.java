package com.starrainnotes.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_admin_invitation")
public class AdminInvitationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long targetAccountId;

    private String emailSnapshot;

    private String targetRoleCode;

    private String tokenHash;

    private String status;

    private Long invitedBy;

    private LocalDateTime expiresAt;

    private Long acceptedBy;

    private LocalDateTime acceptedAt;

    private LocalDateTime revokedAt;

    private LocalDateTime createdAt;

    private LocalDateTime lastSentAt;

    private String mailMessageId;

    private String mailSubmissionStatus;
}
