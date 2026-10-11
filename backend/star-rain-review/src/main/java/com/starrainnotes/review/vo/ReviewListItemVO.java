package com.starrainnotes.review.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * REV-002 待审核列表行。
 *
 * 只含展示快照，不含目标正文：列表不能每行跨模块查询标题，
 * 所以 targetDisplayName 在提交时就冻结下来。真正审核正文要靠详情页
 * 通过 targetRevisionRef 向业务模块取。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListItemVO {

    private Long reviewId;

    private String reviewType;

    private String targetModule;
    private String targetType;
    private Long targetId;
    private String targetDisplayName;

    private Long applicantAccountId;
    private String applicantDisplayName;

    private String status;
    private LocalDateTime submittedAt;
}
