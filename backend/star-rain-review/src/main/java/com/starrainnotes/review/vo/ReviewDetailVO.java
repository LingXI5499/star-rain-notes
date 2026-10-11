package com.starrainnotes.review.vo;

import com.starrainnotes.review.api.ReviewTargetView;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * REV-003 审核详情。
 *
 * 三段拼起来：审核请求元数据 + 目标模块返回的不可变审核视图 + 动作历史。
 * targetView 是业务模块自己的类型，Review 不解析其中字段。
 *
 * canApprove / canReject / canCancel 由 Service 按当前主体的权限与身份算好，
 * 前端不需要自己重算授权规则，避免前后端规则漂移。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDetailVO {

    private Long reviewId;

    private String reviewType;

    private String targetModule;
    private String targetType;
    private Long targetId;
    private String targetRevisionRef;
    private String targetDisplayName;

    private Long applicantAccountId;
    private String applicantDisplayName;

    private String submissionNote;

    private String status;

    private Long reviewerAccountId;
    private String decisionReason;

    private LocalDateTime submittedAt;
    private LocalDateTime decidedAt;
    private LocalDateTime canceledAt;

    // 目标模块提供的不可变审核视图；业务模块没实现 loadReviewView 时为 null
    private ReviewTargetView targetView;

    private List<ReviewActionVO> history;

    // 当前主体能否执行对应动作；PENDING 之外恒为 false
    private boolean canApprove;
    private boolean canReject;
    private boolean canCancel;
}
