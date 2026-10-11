package com.starrainnotes.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_password_reset")
public class PasswordResetEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long accountId;

    private String tokenHash;

    private String status;

    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    private String requestIp;

    private LocalDateTime createdAt;
}
