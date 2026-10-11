/*
 * ReviewCommandInvalidException：审核模块的业务异常文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 内部调用方提交的审核命令不完整。
 *
 * 错误码 REVIEW_COMMAND_INVALID，HTTP 400。
 * 浏览器侧入参由 @Valid 拦住，但 ReviewSubmissionApi 也会被业务模块在服务内部直接调用，
 * 那时不经过任何 Bean Validation，所以 Service 必须自己把关：
 * 缺 targetRevisionRef 就意味着「审核版本未冻结」，这种请求绝不能落库。
 */
/**
 * 审核Command无效异常，属于审核模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class ReviewCommandInvalidException extends ApiException {

    /**
     * 构造ReviewCommandInvalidException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param message 留言；类型为 <code>String</code>
     */
    public ReviewCommandInvalidException(String message) {
        super("REVIEW_COMMAND_INVALID", message, 400);
    }
}
