/*
 * ReviewActionVO：审核模块的响应数据文件。
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
 * 审核动作历史的展示行。
 *
 * actorDisplayName 只有 SUBMITTED 与「申请人取消」能填：这两种动作的执行者就是
 * 审核请求里已快照的申请人。决策类动作只知道 reviewer_account_id，
 * Account 模块没有向其他模块暴露账户摘要 API，因此这里不编造名字，
 * 由前端呈现为「审核员 #<id>」。
 */
/**
 * 审核动作响应对象，属于审核模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewActionVO {

    /**
     * 动作标识。
     */
    private Long actionId;

    /**
     * 动作类型。
     */
    private String actionType;

    /**
     * 操作者账号标识。
     */
    private Long actorAccountId;
    /**
     * 操作者显示名称。
     */
    private String actorDisplayName;
    /**
     * 操作者类型。
     */
    private String actorType;

    /**
     * note。
     */
    private String note;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
