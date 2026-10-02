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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDecisionContext {

    private Long reviewRequestId;

    private String reviewType;

    private ReviewTargetRef target;

    // APPROVED / REJECTED / CANCELED
    private String status;

    private ReviewActionType actionType;

    // 拒绝原因或审核备注；通过时可为空
    private String reason;

    private Long reviewerAccountId;

    // 申请人：业务模块恢复「可重新提交」状态时通常需要通知他
    private Long applicantAccountId;

    private String targetDisplayName;

    private LocalDateTime decidedAt;
}
