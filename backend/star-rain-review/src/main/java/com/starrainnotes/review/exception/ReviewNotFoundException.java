/*
 * ReviewNotFoundException：审核模块的业务异常文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 审核请求不存在。
 *
 * 错误码 REVIEW_NOT_FOUND，HTTP 404。
 */
/**
 * 审核不存在异常，属于审核模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>REVIEW_NOT_FOUND</code>；HTTP 状态：<code>404</code>。
 * 公开提示：审核请求不存在。
 */
public class ReviewNotFoundException extends ApiException {

    /**
     * 构造ReviewNotFoundException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public ReviewNotFoundException() {
        super("REVIEW_NOT_FOUND", "审核请求不存在", 404);
    }
}
