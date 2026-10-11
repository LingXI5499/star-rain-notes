package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 审核请求已经被其他 Reviewer 处理完毕。
 *
 * 错误码 REVIEW_ALREADY_COMPLETED，HTTP 409。
 * 条件 UPDATE 影响 0 行时抛出：这是并发审批唯一成功的判定依据，
 * 绝不允许「先查状态再普通 update」的写法。
 */
public class ReviewAlreadyCompletedException extends ApiException {

    public ReviewAlreadyCompletedException() {
        super("REVIEW_ALREADY_COMPLETED", "该审核请求已被处理，请刷新后查看最新状态", 409);
    }
}
