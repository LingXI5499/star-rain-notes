/*
 * AccountAuditVO：账号模块的响应数据文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.account.vo;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 账号审计响应对象，属于账号模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountAuditVO {

    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
    /**
     * 操作者账号标识。
     */
    private String actorAccountId;
    /**
     * 目标账号标识。
     */
    private String targetAccountId;
    /**
     * 动作代码。
     */
    private String actionCode;
    /**
     * 结果。
     */
    private String result;
    /**
     * IP 地址地址。
     */
    private String ipAddress;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
