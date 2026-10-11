package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 冻结的审核版本已经读不到了。
 *
 * 错误码 REVIEW_TARGET_REVISION_NOT_FOUND，HTTP 404。
 * 与 REVIEW_TARGET_NOT_FOUND 分开：这里是「对象还在，但提交时那个版本没了」，
 * 处理方式不同（需要业务模块补版本或让申请人重新提交），所以错误码必须可区分。
 */
public class ReviewTargetRevisionNotFoundException extends ApiException {

    public ReviewTargetRevisionNotFoundException() {
        super("REVIEW_TARGET_REVISION_NOT_FOUND", "提交时冻结的审核版本已不可读", 404);
    }
}
