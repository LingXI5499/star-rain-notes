/*
 * RolePermissionEntity：账号模块的持久化实体文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 角色权限实体，属于账号模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
@TableName("sr_role_permission")
public class RolePermissionEntity {

    /**
     * 当前业务对象的数据库标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色标识。
     */
    private Long roleId;

    /**
     * 权限标识。
     */
    private Long permissionId;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
