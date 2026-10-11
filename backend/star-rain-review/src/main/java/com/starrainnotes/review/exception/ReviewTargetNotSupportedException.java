package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 没有业务模块声明支持这一类审核目标。
 *
 * 错误码 REVIEW_TARGET_NOT_SUPPORTED，HTTP 400。
 * 说明 targetModule/targetType/reviewType 组合没有任何 ReviewTargetHandler 接单，
 * 通常意味着提交方写错了 reviewType，或者目标模块还没接入 SPI。
 */
public class ReviewTargetNotSupportedException extends ApiException {

    public ReviewTargetNotSupportedException() {
        super("REVIEW_TARGET_NOT_SUPPORTED", "没有已注册的处理器支持该审核目标", 400);
    }
}
