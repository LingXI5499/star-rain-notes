/*
 * ReviewActionType：审核模块的枚举文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */

package com.starrainnotes.review.enumeration;

/*
 * 审核动作类型。
 *
 * 提交时也写一条 SUBMITTED，这样一条请求的完整轨迹是：
 *   Review #100 -> SUBMITTED, REJECTED
 *   Review #105 -> SUBMITTED, APPROVED
 * 历史表只追加，不修改，也不删除。
 */
/**
 * 审核动作类型，属于审核模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum ReviewActionType {
    /**
     * 提交。
     */
    SUBMITTED,
    /**
     * 已批准。
     */
    APPROVED,
    /**
     * 已拒绝。
     */
    REJECTED,
    /**
     * CANCELED。
     */
    CANCELED,
    /**
     * REQUESTED。
     */
    RECOVERY_REQUESTED;

    /**
     * 代码。
     */
    public static final String SUBMITTED_CODE = "SUBMITTED";
    /**
     * 代码。
     */
    public static final String APPROVED_CODE = "APPROVED";
    /**
     * 代码。
     */
    public static final String REJECTED_CODE = "REJECTED";
    /**
     * 代码。
     */
    public static final String CANCELED_CODE = "CANCELED";
}
