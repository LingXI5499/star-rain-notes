/*
 * ReviewAccessDeniedException：审核模块的业务异常文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 查看或操作审核请求的权限不足。
 *
 * 错误码 REVIEW_ACCESS_DENIED，HTTP 403。
 * 用于对象级授权：既不是申请人、也没有 review:read 的账户访问详情或取消请求时抛出。
 */
/**
 * 审核访问被拒绝异常，属于审核模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>REVIEW_ACCESS_DENIED</code>；HTTP 状态：<code>403</code>。
 * 公开提示：没有查看该审核请求的权限。
 */
public class ReviewAccessDeniedException extends ApiException {

    /**
     * 构造ReviewAccessDeniedException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public ReviewAccessDeniedException() {
        super("REVIEW_ACCESS_DENIED", "没有查看该审核请求的权限", 403);
    }
}
