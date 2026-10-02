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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummary {

    private Long reviewRequestId;

    private String reviewType;

    private String targetModule;
    private String targetType;
    private Long targetId;
    private String targetRevisionRef;

    private String status;

    private Long applicantAccountId;

    private LocalDateTime submittedAt;
    private LocalDateTime decidedAt;

    // 是否为终态：业务模块据此决定要不要再提交一次
    public boolean terminal() {
        return ReviewStatus.isTerminal(status);
    }
}
