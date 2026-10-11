/*
 * ReviewDetailVO：审核模块的响应数据文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 * 主要协作类型：ReviewTargetView。
 */

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
/**
 * 审核详情响应对象，属于审核模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDetailVO {

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
     * submissionNote。
     */
    private String submissionNote;

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

    // 目标模块提供的不可变审核视图；业务模块没实现 loadReviewView 时为 null
    /**
     * 目标View。
     */
    private ReviewTargetView targetView;

    /**
     * 历史。
     */
    private List<ReviewActionVO> history;

    // 当前主体能否执行对应动作；PENDING 之外恒为 false
    /**
     * canApprove。
     */
    private boolean canApprove;
    /**
     * canReject。
     */
    private boolean canReject;
    /**
     * canCancel。
     */
    private boolean canCancel;
}
