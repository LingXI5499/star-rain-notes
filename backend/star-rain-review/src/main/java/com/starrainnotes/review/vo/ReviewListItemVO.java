/*
 * ReviewListItemVO：审核模块的响应数据文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

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
/**
 * 审核ListItem响应对象，属于审核模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListItemVO {

    /**
     * 审核标识。
     */
    private Long reviewId;

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
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 提交时间。
     */
    private LocalDateTime submittedAt;
}
