package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 只有申请人本人可以取消自己提交的待审请求。
 *
 * 错误码 REVIEW_APPLICANT_MISMATCH，HTTP 403。
 * 与 REVIEW_ACCESS_DENIED 分开：这里的语义是「身份对不上」，
 * 而不是「你没有这个权限」，便于前端给出准确提示。
 */
public class ReviewApplicantMismatchException extends ApiException {

    public ReviewApplicantMismatchException() {
        super("REVIEW_APPLICANT_MISMATCH", "只有申请人本人可以取消该审核请求", 403);
    }
}
