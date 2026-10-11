/*
 * ReviewRejectCommand：审核模块的请求与传输数据文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * REV-005 审核拒绝入参。
 *
 * reason 必填：拒绝原因会原样写进 decision_reason 与动作历史，
 * 是申请人修改后重新提交的唯一依据。Service 仍会再校验一次，
 * 因为 ReviewRejectCommand 也可能由内部调用方直接构造。
 */
/**
 * 审核RejectCommand，属于审核模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRejectCommand {

    /**
     * 原因。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Size(max = 2000)</code>。
     */
    @NotBlank
    @Size(max = 2000)
    private String reason;
}
