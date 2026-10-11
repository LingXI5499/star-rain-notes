package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 内部调用方提交的审核命令不完整。
 *
 * 错误码 REVIEW_COMMAND_INVALID，HTTP 400。
 * 浏览器侧入参由 @Valid 拦住，但 ReviewSubmissionApi 也会被业务模块在服务内部直接调用，
 * 那时不经过任何 Bean Validation，所以 Service 必须自己把关：
 * 缺 targetRevisionRef 就意味着「审核版本未冻结」，这种请求绝不能落库。
 */
public class ReviewCommandInvalidException extends ApiException {

    public ReviewCommandInvalidException(String message) {
        super("REVIEW_COMMAND_INVALID", message, 400);
    }
}
