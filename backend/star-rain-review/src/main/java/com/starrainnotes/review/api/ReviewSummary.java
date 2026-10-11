/*
 * ReviewSummary：审核模块的跨模块接口文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：ReviewStatus。
 * 阅读入口：terminal（terminal处理入口）。
 */

package com.starrainnotes.review.api;

import com.starrainnotes.review.enumeration.ReviewStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 审核请求摘要。
 *
 * 业务模块拿它回答「这个目标当前有没有待审、上一次审核结论是什么」，
 * 但业务模块自己的状态仍由业务模块推进，Review 不会替它改状态。
 */
/**
 * 审核摘要，属于审核模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummary {

    /**
     * 审核请求标识。
     */
    private Long reviewRequestId;

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
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;

    /**
     * 申请人账号标识。
     */
    private Long applicantAccountId;

    /**
     * 提交时间。
     */
    private LocalDateTime submittedAt;
    /**
     * 决定时间。
     */
    private LocalDateTime decidedAt;

    // 是否为终态：业务模块据此决定要不要再提交一次
    /**
     * terminal处理入口。
     * 关键处理步骤调用 <code>ReviewStatus.isTerminal</code>。
     * @return 本方法条件是否成立的判断结果
     */
    public boolean terminal() {
        return ReviewStatus.isTerminal(status);
    }
}
