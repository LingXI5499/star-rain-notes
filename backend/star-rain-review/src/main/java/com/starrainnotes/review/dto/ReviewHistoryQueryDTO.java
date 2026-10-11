package com.starrainnotes.review.dto;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * REV-007 审核历史查询条件。
 *
 * 比待审列表多出状态、申请人/决策人、目标定位与时间范围，
 * 用于「这个 Tutorial 历史上被审过几次、结论分别是什么」这类追溯。
 * 时间范围是闭区间，按 submitted_at 过滤。
 *
 * 同样不做 bean validation：非法取值统一由 Service 抛 REVIEW_QUERY_INVALID。
 */
@Data
public class ReviewHistoryQueryDTO {

    private int page = 1;

    private int pageSize = 20;

    private String targetModule;

    private String targetType;

    private Long targetId;

    private String reviewType;

    // PENDING / APPROVED / REJECTED / CANCELED
    private String status;

    private Long applicantAccountId;

    private Long reviewerAccountId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
