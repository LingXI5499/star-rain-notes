/*
 * ReviewHistoryVO：审核模块的响应数据文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.review.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * REV-007 审核历史行。
 *
 * 与列表行的差别：历史要把决策人、决定原因与三个时间点都摊开，
 * 因为追溯时最常问的是「谁在什么时候因为什么做了决定」。
 * 决策人显示名用决策时落库的快照，账户改名不会污染历史。
 */
/**
 * 审核历史响应对象，属于审核模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewHistoryVO {

    /**
     * 审核标识。
     */
    private Long reviewId;

    /**
     * 审核类型。
     */
    private String reviewType;

    /**
     * 目标对象所属模块。
     */
    private String targetModule;
    /**
     * 目标对象的业务类型。
     */
    private String targetType;
    /**
     * 被处理或审核的目标对象标识。
     */
    private Long targetId;
    /**
     * 审核目标对应的修订引用。
     */
    private String targetRevisionRef;
    /**
     * 目标显示名称。
     */
    private String targetDisplayName;

    /**
     * 申请人账号标识。
     */
    private Long applicantAccountId;
    /**
     * 申请人显示名称。
     */
    private String applicantDisplayName;

    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;

    /**
     * 审核人账号标识。
     */
    private Long reviewerAccountId;
    /**
     * 决定原因。
     */
    private String decisionReason;

    /**
     * 提交时间。
     */
    private LocalDateTime submittedAt;
    /**
     * 决定时间。
     */
    private LocalDateTime decidedAt;
    /**
     * canceled时间。
     */
    private LocalDateTime canceledAt;
}
