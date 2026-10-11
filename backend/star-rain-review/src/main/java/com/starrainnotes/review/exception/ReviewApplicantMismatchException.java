/*
 * ReviewApplicantMismatchException：审核模块的业务异常文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 只有申请人本人可以取消自己提交的待审请求。
 *
 * 错误码 REVIEW_APPLICANT_MISMATCH，HTTP 403。
 * 与 REVIEW_ACCESS_DENIED 分开：这里的语义是「身份对不上」，
 * 而不是「你没有这个权限」，便于前端给出准确提示。
 */
/**
 * 审核申请人不匹配异常，属于审核模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>REVIEW_APPLICANT_MISMATCH</code>；HTTP 状态：<code>403</code>。
 * 公开提示：只有申请人本人可以取消该审核请求。
 */
public class ReviewApplicantMismatchException extends ApiException {

    /**
     * 构造ReviewApplicantMismatchException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public ReviewApplicantMismatchException() {
        super("REVIEW_APPLICANT_MISMATCH", "只有申请人本人可以取消该审核请求", 403);
    }
}
