/*
 * EmailVerificationEntity：账号模块的持久化实体文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.account.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 邮箱验证实体，属于账号模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class EmailVerificationEntity {
    /**
     * 邮箱。
     */
    private String email;
    /**
     * 代码摘要。
     */
    private String codeHash;
    /**
     * 到期时间。
     */
    private LocalDateTime expiresAt;
    /**
     * consumed时间。
     */
    private LocalDateTime consumedAt;
    /**
     * sent时间。
     */
    private LocalDateTime sentAt;
    /**
     * 时间窗口Started时间。
     */
    private LocalDateTime windowStartedAt;
    /**
     * send数量。
     */
    private int sendCount;
    /**
     * 失败Attempts。
     */
    private int failedAttempts;
}
