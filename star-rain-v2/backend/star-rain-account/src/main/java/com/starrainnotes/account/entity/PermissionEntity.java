package com.starrainnotes.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_permission")
public class PermissionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String resource;

    private String action;

    private String name;

    private String description;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
