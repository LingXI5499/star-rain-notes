package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 查看或操作审核请求的权限不足。
 *
 * 错误码 REVIEW_ACCESS_DENIED，HTTP 403。
 * 用于对象级授权：既不是申请人、也没有 review:read 的账户访问详情或取消请求时抛出。
 */
public class ReviewAccessDeniedException extends ApiException {

    public ReviewAccessDeniedException() {
        super("REVIEW_ACCESS_DENIED", "没有查看该审核请求的权限", 403);
    }
}
