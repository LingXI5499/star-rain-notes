/*
 * ReviewNotPendingException：审核模块的业务异常文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 只有 PENDING 可以取消。
 *
 * 错误码 REVIEW_NOT_PENDING，HTTP 409。
 * 与 REVIEW_ALREADY_COMPLETED 分开，是因为调用方（申请人）需要区分
 * 「我点太晚了，别人已经审完」和「这条本来就不是待审状态」。
 */
/**
 * 审核不待处理异常，属于审核模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>REVIEW_NOT_PENDING</code>；HTTP 状态：<code>409</code>。
 * 公开提示：只有待审核状态的请求可以取消。
 */
public class ReviewNotPendingException extends ApiException {

    /**
     * 构造ReviewNotPendingException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public ReviewNotPendingException() {
        super("REVIEW_NOT_PENDING", "只有待审核状态的请求可以取消", 409);
    }
}
