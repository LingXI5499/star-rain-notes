package com.starrainnotes.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_account")
public class AccountEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;

    private LocalDateTime emailVerifiedAt;

    private String displayName;

    private String status;

    private Integer authVersion;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
