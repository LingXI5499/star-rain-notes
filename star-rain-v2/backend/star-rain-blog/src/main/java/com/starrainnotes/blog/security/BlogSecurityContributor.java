package com.starrainnotes.blog.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/*
 * Blog 模块的 URL 边界。
 *
 * 前台公开读取必须允许匿名到达 Controller：/api/public/blog/** 只返回 status = PUBLISHED 的内容，
 * “草稿与已撤回文章不可见”是每个资源自己的状态属性，URL 层无法预判，
 * 因此放行到 Controller，由 Service 在查询条件里强制 status = PUBLISHED。
 *
 * 后台接口只要求已认证，具体权限由方法级 @PreAuthorize 执行：
 * 这里若写成 permitAll，博客就会变成任何人可写的公开内容域。
 */
@Component
public class BlogSecurityContributor implements ModuleSecurityContributor {

    @Override
    public String moduleName() {
        return "blog";
    }

    // Blog 依赖 Account 的认证上下文与 Media 的公开内容地址，排在它们之后
    @Override
    public int order() {
        return 40;
    }

    @Override
    public List<String> publicPatterns() {
        return List.of("/api/public/blog/**");
    }

    @Override
    public List<String> authenticatedPatterns() {
        return List.of("/api/admin/blog/**");
    }
}
