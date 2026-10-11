package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 审核查询参数非法。
 *
 * 错误码 REVIEW_QUERY_INVALID，HTTP 400。
 * 与 Media 模块同一口径：分页越界、状态取值非法等一律明确报错，不静默纠正，
 * 避免前端拿到「看起来正常但被悄悄改了条件」的结果。
 */
public class ReviewQueryInvalidException extends ApiException {

    public ReviewQueryInvalidException(String message) {
        super("REVIEW_QUERY_INVALID", message, 400);
    }
}
