/*
 * ReviewAlreadyCompletedException：审核模块的业务异常文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 审核请求已经被其他 Reviewer 处理完毕。
 *
 * 错误码 REVIEW_ALREADY_COMPLETED，HTTP 409。
 * 条件 UPDATE 影响 0 行时抛出：这是并发审批唯一成功的判定依据，
 * 绝不允许「先查状态再普通 update」的写法。
 */
/**
 * 审核Already已完成异常，属于审核模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>REVIEW_ALREADY_COMPLETED</code>；HTTP 状态：<code>409</code>。
 * 公开提示：该审核请求已被处理，请刷新后查看最新状态。
 */
public class ReviewAlreadyCompletedException extends ApiException {

    /**
     * 构造ReviewAlreadyCompletedException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public ReviewAlreadyCompletedException() {
        super("REVIEW_ALREADY_COMPLETED", "该审核请求已被处理，请刷新后查看最新状态", 409);
    }
}
