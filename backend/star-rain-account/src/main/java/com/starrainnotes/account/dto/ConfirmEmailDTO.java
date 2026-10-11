/*
 * ConfirmEmailDTO：账号模块的请求与传输数据文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 确认邮箱传输对象，属于账号模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmEmailDTO {

    /**
     * 验证代码。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Pattern(regexp = "[0-9]{6}")</code>。
     */
    @NotBlank
    @Pattern(regexp = "[0-9]{6}")
    private String verificationCode;
}
