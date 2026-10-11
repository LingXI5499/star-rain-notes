/*
 * ReviewTargetRevisionNotFoundException：审核模块的业务异常文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 冻结的审核版本已经读不到了。
 *
 * 错误码 REVIEW_TARGET_REVISION_NOT_FOUND，HTTP 404。
 * 与 REVIEW_TARGET_NOT_FOUND 分开：这里是「对象还在，但提交时那个版本没了」，
 * 处理方式不同（需要业务模块补版本或让申请人重新提交），所以错误码必须可区分。
 */
/**
 * 审核目标修订不存在异常，属于审核模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>REVIEW_TARGET_REVISION_NOT_FOUND</code>；HTTP 状态：<code>404</code>。
 * 公开提示：提交时冻结的审核版本已不可读。
 */
public class ReviewTargetRevisionNotFoundException extends ApiException {

    /**
     * 构造ReviewTargetRevisionNotFoundException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public ReviewTargetRevisionNotFoundException() {
        super("REVIEW_TARGET_REVISION_NOT_FOUND", "提交时冻结的审核版本已不可读", 404);
    }
}
