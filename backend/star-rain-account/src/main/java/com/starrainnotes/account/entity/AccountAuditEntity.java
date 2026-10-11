/*
 * AccountAuditEntity：账号模块的持久化实体文件。
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
 * 账号审计实体，属于账号模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
@TableName("sr_account_audit")
public class AccountAuditEntity {

    /**
     * 当前业务对象的数据库标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作者账号标识。
     */
    private Long actorAccountId;

    /**
     * 目标账号标识。
     */
    private Long targetAccountId;

    /**
     * 动作代码。
     */
    private String actionCode;

    /**
     * 结果。
     */
    private String result;

    /**
     * 详情JSON。
     */
    private String detailJson;

    /**
     * IP 地址地址。
     */
    private String ipAddress;

    /**
     * 用户Agent。
     */
    private String userAgent;

    /**
     * 请求标识。
     */
    private String requestId;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
