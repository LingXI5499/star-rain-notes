package com.starrainnotes.review.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 审核动作历史的展示行。
 *
 * actorDisplayName 只有 SUBMITTED 与「申请人取消」能填：这两种动作的执行者就是
 * 审核请求里已快照的申请人。决策类动作只知道 reviewer_account_id，
 * Account 模块没有向其他模块暴露账户摘要 API，因此这里不编造名字，
 * 由前端呈现为「审核员 #<id>」。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewActionVO {

    private Long actionId;

    private String actionType;

    private Long actorAccountId;
    private String actorDisplayName;
    private String actorType;

    private String note;

    private LocalDateTime createdAt;
}
