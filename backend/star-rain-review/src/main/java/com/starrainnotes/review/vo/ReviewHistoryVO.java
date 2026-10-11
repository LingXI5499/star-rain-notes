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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewHistoryVO {

    private Long reviewId;

    private String reviewType;

    private String targetModule;
    private String targetType;
    private Long targetId;
    private String targetRevisionRef;
    private String targetDisplayName;

    private Long applicantAccountId;
    private String applicantDisplayName;

    private String status;

    private Long reviewerAccountId;
    private String decisionReason;

    private LocalDateTime submittedAt;
    private LocalDateTime decidedAt;
    private LocalDateTime canceledAt;
}
