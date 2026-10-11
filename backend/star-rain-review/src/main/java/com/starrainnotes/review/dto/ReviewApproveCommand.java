/*
 * ReviewApproveCommand：审核模块的请求与传输数据文件。
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
 * REV-004 审核通过入参。
 *
 * note 可选：通过时通常不需要解释，但允许 Reviewer 留一句审核备注，
 * 会写进 sr_review_action.note 供后续追溯。
 */
/**
 * 审核ApproveCommand，属于审核模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewApproveCommand {

    /**
     * note。
     * 校验规则 <code>@Size(max = 2000)</code>。
     */
    @Size(max = 2000)
    private String note;
}
