/*
 * ReviewStatus：审核模块的枚举文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 * 阅读入口：isTerminal（判断是否Terminal）；isKnown（判断是否Known）。
 */

package com.starrainnotes.review.enumeration;

/*
 * 审核请求状态机。
 *
 * 不存在 --submit--> PENDING
 * PENDING --approve--> APPROVED
 * PENDING --reject --> REJECTED
 * PENDING --cancel --> CANCELED
 *
 * APPROVED / REJECTED / CANCELED 都是终态，禁止回退到 PENDING；
 * 重新提交一律创建新的 ReviewRequest，不复用旧请求。
 */
/**
 * 审核状态，属于审核模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum ReviewStatus {
    /**
     * 待处理。
     */
    PENDING,
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
    CANCELED;

    // 数据库与 XML 中统一使用字符串常量，避免各处硬编码字面量写错
    /**
     * 代码。
     */
    public static final String PENDING_CODE = "PENDING";
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

    // 只有 PENDING 可以发生状态变更，其余都是终态
    /**
     * 判断是否Terminal。
     * 主要协作调用：<code>APPROVED_CODE.equals</code>、<code>REJECTED_CODE.equals</code>、<code>CANCELED_CODE.equals</code>。
     *
     * @param code 代码；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    public static boolean isTerminal(String code) {
        return APPROVED_CODE.equals(code)
                || REJECTED_CODE.equals(code)
                || CANCELED_CODE.equals(code);
    }

    /**
     * 判断是否Known。
     * 主要协作调用：<code>PENDING_CODE.equals</code>。
     *
     * @param code 代码；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    public static boolean isKnown(String code) {
        return PENDING_CODE.equals(code) || isTerminal(code);
    }
}
