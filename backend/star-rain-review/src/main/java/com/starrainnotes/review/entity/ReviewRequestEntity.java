/*
 * ReviewRequestEntity：审核模块的持久化实体文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

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
/**
 * 审核请求实体，属于审核模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
@TableName("sr_review_request")
public class ReviewRequestEntity {

    /**
     * 当前业务对象的数据库标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
