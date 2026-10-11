/*
 * ReviewHistoryQueryDTO：审核模块的请求与传输数据文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

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
/**
 * 审核历史查询参数传输对象，属于审核模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class ReviewHistoryQueryDTO {

    /**
     * 分页页码。
     * 初始值为 <code>1</code>。
     */
    private int page = 1;

    /**
     * 单页返回条目数。
     * 初始值为 <code>20</code>。
     */
    private int pageSize = 20;

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
     * 审核类型。
     */
    private String reviewType;

    // PENDING / APPROVED / REJECTED / CANCELED
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;

    /**
     * 申请人账号标识。
     */
    private Long applicantAccountId;

    /**
     * 审核人账号标识。
     */
    private Long reviewerAccountId;

    /**
     * 开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 结束时间。
     */
    private LocalDateTime endTime;
}
