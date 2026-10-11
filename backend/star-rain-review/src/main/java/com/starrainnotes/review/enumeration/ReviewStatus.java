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
public enum ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELED;

    // 数据库与 XML 中统一使用字符串常量，避免各处硬编码字面量写错
    public static final String PENDING_CODE = "PENDING";
    public static final String APPROVED_CODE = "APPROVED";
    public static final String REJECTED_CODE = "REJECTED";
    public static final String CANCELED_CODE = "CANCELED";

    // 只有 PENDING 可以发生状态变更，其余都是终态
    public static boolean isTerminal(String code) {
        return APPROVED_CODE.equals(code)
                || REJECTED_CODE.equals(code)
                || CANCELED_CODE.equals(code);
    }

    public static boolean isKnown(String code) {
        return PENDING_CODE.equals(code) || isTerminal(code);
    }
}
