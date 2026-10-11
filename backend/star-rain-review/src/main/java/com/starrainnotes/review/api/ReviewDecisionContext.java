/*
 * ReviewDecisionContext：审核模块的跨模块接口文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：ReviewActionType。
 */

package com.starrainnotes.review.api;

import com.starrainnotes.review.enumeration.ReviewActionType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 审核决定上下文。
 *
 * 这是 Review 传给目标业务模块的唯一回调载荷：业务模块据此推进自己的状态，
 * 但决定「推进成什么状态」的规则属于业务模块，Review 不参与判断。
 *
 * 回调发生在审核决定所在事务提交之后（afterCommit），
 * 因此这里给出的 reviewRequestId / status 一定已经在数据库里可见。
 */
/**
 * 审核决定Context，属于审核模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDecisionContext {

    /**
     * 审核请求标识。
     */
    private Long reviewRequestId;

    /**
     * 审核类型。
     */
    private String reviewType;

    /**
     * 目标。
     */
    private ReviewTargetRef target;

    // APPROVED / REJECTED / CANCELED
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;

    /**
     * 动作类型。
     */
    private ReviewActionType actionType;

    // 拒绝原因或审核备注；通过时可为空
    /**
     * 原因。
     */
    private String reason;

    /**
     * 审核人账号标识。
     */
    private Long reviewerAccountId;

    // 申请人：业务模块恢复「可重新提交」状态时通常需要通知他
    /**
     * 申请人账号标识。
     */
    private Long applicantAccountId;

    /**
     * 目标显示名称。
     */
    private String targetDisplayName;

    /**
     * 决定时间。
     */
    private LocalDateTime decidedAt;
}
