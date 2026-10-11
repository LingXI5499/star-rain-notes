/*
 * ReviewSubmissionCommand：审核模块的请求与传输数据文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.review.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 提交审核的入参。
 *
 * 这个对象由业务 Service 构造，不是让浏览器任意提交 targetModule/targetId：
 * 业务模块必须先验证 actor 与对象级权限，再确定不可变 revisionRef，最后才调用
 * ReviewSubmissionApi.submit()。
 *
 * applicantAccountId 由业务模块从当前认证主体取值，浏览器无法伪造。
 */
/**
 * 审核SubmissionCommand，属于审核模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmissionCommand {

    /**
     * 审核类型。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Size(max = 100)</code>。
     */
    @NotBlank
    @Size(max = 100)
    private String reviewType;

    /**
     * 目标对象所属模块。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Size(max = 50)</code>。
     */
    @NotBlank
    @Size(max = 50)
    private String targetModule;

    /**
     * 目标对象的业务类型。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Size(max = 50)</code>。
     */
    @NotBlank
    @Size(max = 50)
    private String targetType;

    /**
     * 被处理或审核的目标对象标识。
     * 不允许为 null。
     */
    @NotNull
    private Long targetId;

    // 提交时不可变版本引用；允许为空时业务模块必须自己保证「审核的就是当前版本」
    /**
     * 审核目标对应的修订引用。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Size(max = 128)</code>。
     */
    @NotBlank
    @Size(max = 128)
    private String targetRevisionRef;

    /**
     * 目标显示名称。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Size(max = 255)</code>。
     */
    @NotBlank
    @Size(max = 255)
    private String targetDisplayName;

    /**
     * 申请人账号标识。
     * 不允许为 null。
     */
    @NotNull
    private Long applicantAccountId;

    /**
     * 申请人显示名称。
     * 校验规则 <code>@Size(max = 100)</code>。
     */
    @Size(max = 100)
    private String applicantDisplayName;

    /**
     * submissionNote。
     * 校验规则 <code>@Size(max = 1000)</code>。
     */
    @Size(max = 1000)
    private String submissionNote;
}
