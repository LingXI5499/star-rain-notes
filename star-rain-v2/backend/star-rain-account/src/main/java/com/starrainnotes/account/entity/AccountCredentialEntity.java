package com.starrainnotes.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_account_credential")
public class AccountCredentialEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long accountId;

    private String passwordHash;

    private LocalDateTime passwordChangedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
