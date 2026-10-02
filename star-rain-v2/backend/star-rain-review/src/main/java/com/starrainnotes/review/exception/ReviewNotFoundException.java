package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 审核请求不存在。
 *
 * 错误码 REVIEW_NOT_FOUND，HTTP 404。
 */
public class ReviewNotFoundException extends ApiException {

    public ReviewNotFoundException() {
        super("REVIEW_NOT_FOUND", "审核请求不存在", 404);
    }
}
