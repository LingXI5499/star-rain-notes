package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 同一目标 + reviewType 已经存在待审请求。
 *
 * 错误码 REVIEW_ALREADY_PENDING，HTTP 409。
 * 这是并发重复提交与用户重复点击共同的兜底语义：重新提交前必须先取消或等结论。
 */
public class ReviewAlreadyPendingException extends ApiException {

    public ReviewAlreadyPendingException() {
        super("REVIEW_ALREADY_PENDING", "该目标已存在待审核请求，不能重复提交", 409);
    }
}
