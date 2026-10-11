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
public class BlogAccessDeniedException extends ApiException {

    public BlogAccessDeniedException() {
        super("BLOG_ACCESS_DENIED", "只有超级管理员可以执行该操作", 403);
    }
}
