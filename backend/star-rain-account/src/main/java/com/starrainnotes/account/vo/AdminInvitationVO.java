/*
 * AdminInvitationVO：账号模块的响应数据文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.account.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 后台邀请响应对象，属于账号模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminInvitationVO {

    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
    /**
     * 目标账号标识。
     */
    private String targetAccountId;
    /**
     * 到期时间。
     */
    private LocalDateTime expiresAt;
    /**
     * 邀请地址。
     */
    private String invitationUrl;
    /**
     * 目标邮箱。
     */
    private String targetEmail;
    /**
     * 邮件Submission状态。
     */
    private String mailSubmissionStatus;
}
