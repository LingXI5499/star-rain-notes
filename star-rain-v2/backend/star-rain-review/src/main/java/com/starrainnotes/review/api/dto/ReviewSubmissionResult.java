package com.starrainnotes.review.api.dto;

import com.starrainnotes.review.enumeration.ReviewStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 提交审核的结果。
 *
 * 业务模块拿 reviewRequestId 做双向关联（例如写进自己的 under_review 记录），
 * 也应该把 status 原样存下来，避免每次展示都回查 Review。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmissionResult {

    private Long reviewRequestId;

    private String status;

    private String reviewType;

    private String targetModule;
    private String targetType;
    private Long targetId;
    private String targetRevisionRef;

    private LocalDateTime submittedAt;

    public boolean pending() {
        return ReviewStatus.PENDING_CODE.equals(status);
    }
}
