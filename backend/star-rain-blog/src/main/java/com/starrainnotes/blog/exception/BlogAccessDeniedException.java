/*
 * BlogAccessDeniedException：博客模块的业务异常文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 当前操作者没有博客管理资格。
 *
 * 与 Spring Security 的 403 FORBIDDEN 区别：这里表达的是“权限码之外的角色约束”。
 * 规范 §3 明确只有 Super Admin 能发布，因此即使某天有人把 blog:publish 授给了别的角色，
 * 服务层仍会独立拦下 —— 两条判断都成立才算通过。
 * 错误码 BLOG_ACCESS_DENIED，HTTP 403。
 */
/**
 * 博客访问被拒绝异常，属于博客模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>BLOG_ACCESS_DENIED</code>；HTTP 状态：<code>403</code>。
 * 公开提示：只有超级管理员可以执行该操作。
 */
public class BlogAccessDeniedException extends ApiException {

    /**
     * 构造BlogAccessDeniedException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public BlogAccessDeniedException() {
        super("BLOG_ACCESS_DENIED", "只有超级管理员可以执行该操作", 403);
    }
}
