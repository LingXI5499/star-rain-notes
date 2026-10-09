package com.starrainnotes.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/*
 * 审核请求。
 *
 * 只回答「谁提交 / 审核什么 / 审核哪一个版本 / 当前状态 / 最终谁决定」，
 * 不持有任何目标业务对象的正文与发布状态——那是业务模块自己的事。
 *
 * target_revision_ref 是提交时冻结的不可变版本引用：Reviewer 打开详情时，
 * 业务模块必须按这个引用返回当时的审核视图，而不是实时读当前数据。
 */
@Data
@TableName("sr_review_request")
public class ReviewRequestEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
