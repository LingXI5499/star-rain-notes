/*
 * ReviewTargetNotSupportedException：审核模块的业务异常文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 没有业务模块声明支持这一类审核目标。
 *
 * 错误码 REVIEW_TARGET_NOT_SUPPORTED，HTTP 400。
 * 说明 targetModule/targetType/reviewType 组合没有任何 ReviewTargetHandler 接单，
 * 通常意味着提交方写错了 reviewType，或者目标模块还没接入 SPI。
 */
/**
 * 审核目标不Supported异常，属于审核模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>REVIEW_TARGET_NOT_SUPPORTED</code>；HTTP 状态：<code>400</code>。
 * 公开提示：没有已注册的处理器支持该审核目标。
 */
public class ReviewTargetNotSupportedException extends ApiException {

    /**
     * 构造ReviewTargetNotSupportedException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public ReviewTargetNotSupportedException() {
        super("REVIEW_TARGET_NOT_SUPPORTED", "没有已注册的处理器支持该审核目标", 400);
    }
}
