/*
 * ReviewTargetNotFoundException：审核模块的业务异常文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 目标业务对象不存在（已被删除）。
 *
 * 错误码 REVIEW_TARGET_NOT_FOUND，HTTP 404。
 *
 * 与 REVIEW_TARGET_REVISION_NOT_FOUND 的分工：
 *   本条用于「对象本身没了」，那条用于「对象还在、提交时冻结的版本读不到了」。
 *   业务模块通过 loadReviewView 返回 null 表达「读不到」，
 *   因此 Review 侧当前统一按 REVISION_NOT_FOUND 报出；
 *   本错误码保留给将来业务模块显式区分「对象已删除」的场景，
 *   以免两种语义被迫挤进同一个码里，前端无法给出不同引导。
 */
/**
 * 审核目标不存在异常，属于审核模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>REVIEW_TARGET_NOT_FOUND</code>；HTTP 状态：<code>404</code>。
 * 公开提示：审核目标不存在或已被删除。
 */
public class ReviewTargetNotFoundException extends ApiException {

    /**
     * 构造ReviewTargetNotFoundException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public ReviewTargetNotFoundException() {
        super("REVIEW_TARGET_NOT_FOUND", "审核目标不存在或已被删除", 404);
    }
}
