package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 同一条审核请求被并发提交了多个决定。
 *
 * 错误码 REVIEW_DECISION_CONFLICT，HTTP 409。
 * 与 REVIEW_ALREADY_COMPLETED 的区别：这里强调「同一请求在本事务内出现了
 * 不一致的写入结果」，作为条件更新与动作历史数量不匹配时的最后一道兜底，
 * 宁可报冲突也不留下「状态改了但历史没写」的脏轨迹。
 */
public class ReviewDecisionConflictException extends ApiException {

    public ReviewDecisionConflictException() {
        super("REVIEW_DECISION_CONFLICT", "该审核请求正在被处理，请稍后重试", 409);
    }
}
