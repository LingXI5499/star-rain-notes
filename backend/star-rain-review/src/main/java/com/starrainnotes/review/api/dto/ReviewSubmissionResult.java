/*
 * ReviewSubmissionResult：审核模块的请求与传输数据文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 * 主要协作类型：ReviewStatus。
 * 阅读入口：pending（待处理处理入口）。
 */

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
/**
 * 审核Submission结果，属于审核模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmissionResult {

    /**
     * 审核请求标识。
     */
    private Long reviewRequestId;

    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;

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
     * 提交时间。
     */
    private LocalDateTime submittedAt;

    /**
     * 待处理处理入口。
     * @return 本方法条件是否成立的判断结果
     */
    public boolean pending() {
        return ReviewStatus.PENDING_CODE.equals(status);
    }
}
