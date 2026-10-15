/*
 * EmailVerificationCodeVO：账号模块的响应数据文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.account.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮箱验证代码响应对象，属于账号模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationCodeVO {

    /**
     * 到期内部秒。
     */
    private int expiresInSeconds;
    /**
     * resend之后秒。
     */
    private int resendAfterSeconds;
}
