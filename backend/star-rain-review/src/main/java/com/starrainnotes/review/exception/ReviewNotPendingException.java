package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 只有 PENDING 可以取消。
 *
 * 错误码 REVIEW_NOT_PENDING，HTTP 409。
 * 与 REVIEW_ALREADY_COMPLETED 分开，是因为调用方（申请人）需要区分
 * 「我点太晚了，别人已经审完」和「这条本来就不是待审状态」。
 */
public class ReviewNotPendingException extends ApiException {

    public ReviewNotPendingException() {
        super("REVIEW_NOT_PENDING", "只有待审核状态的请求可以取消", 409);
    }
}
