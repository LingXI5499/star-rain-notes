package com.starrainnotes.review.enumeration;

/*
 * 审核动作类型。
 *
 * 提交时也写一条 SUBMITTED，这样一条请求的完整轨迹是：
 *   Review #100 -> SUBMITTED, REJECTED
 *   Review #105 -> SUBMITTED, APPROVED
 * 历史表只追加，不修改，也不删除。
 */
public enum ReviewActionType {
    SUBMITTED,
    APPROVED,
    REJECTED,
    CANCELED;

    public static final String SUBMITTED_CODE = "SUBMITTED";
    public static final String APPROVED_CODE = "APPROVED";
    public static final String REJECTED_CODE = "REJECTED";
    public static final String CANCELED_CODE = "CANCELED";
}
