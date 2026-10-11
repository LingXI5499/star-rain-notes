/*
 * AdminInvitationEntity：账号模块的持久化实体文件。
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
 * 后台邀请实体，属于账号模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
@TableName("sr_admin_invitation")
public class AdminInvitationEntity {

    /**
     * 当前业务对象的数据库标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 目标账号标识。
     */
    private Long targetAccountId;

    /**
     * 邮箱快照。
     */
    private String emailSnapshot;

    /**
     * 目标角色代码。
     */
    private String targetRoleCode;

    /**
     * 令牌摘要。
     */
    private String tokenHash;

    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;

    /**
     * invited依据。
     */
    private Long invitedBy;

    /**
     * 到期时间。
     */
    private LocalDateTime expiresAt;

    /**
     * 已接受依据。
     */
    private Long acceptedBy;

    /**
     * 已接受时间。
     */
    private LocalDateTime acceptedAt;

    /**
     * revoked时间。
     */
    private LocalDateTime revokedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 最近Sent时间。
     */
    private LocalDateTime lastSentAt;

    /**
     * 邮件留言标识。
     */
    private String mailMessageId;

    /**
     * 邮件Submission状态。
     */
    private String mailSubmissionStatus;
}
