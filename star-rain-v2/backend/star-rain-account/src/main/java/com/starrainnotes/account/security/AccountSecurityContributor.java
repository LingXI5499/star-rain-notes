package com.starrainnotes.account.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/*
 * Account 模块自身的 URL 边界声明。
 *
 * 规则与重构前 AccountSecurityConfig 内联的写法逐条一致，语义未变：
 * 角色/权限配置类接口直接拒绝，认证入口匿名放行，其余账户与后台接口只要求已认证。
 */
@Component
public class AccountSecurityContributor implements ModuleSecurityContributor {

    @Override
    public String moduleName() {
        return "account";
    }

    // Account 是基础模块，排在业务模块之前
    @Override
    public int order() {
        return 10;
    }

    // 旧的动态角色/权限配置接口已废弃：直接拒绝，连已认证也不放行
    @Override
    public List<String> deniedPatterns() {
        return List.of(
                "/api/admin/roles", "/api/admin/roles/**", "/api/admin/permissions",
                "/api/admin/accounts/*/roles");
    }

    // 注册、登录、邮箱验证码、密码重置与 CSRF 令牌获取允许匿名
    @Override
    public List<String> publicPatterns() {
        return List.of(
                "/api/auth/register", "/api/auth/login",
                "/api/auth/register/verification-codes",
                "/api/auth/password-reset/**", "/api/auth/csrf");
    }

    // 自助账户操作与后台账户管理只要求已认证，具体权限由方法级 @PreAuthorize 判断
    @Override
    public List<String> authenticatedPatterns() {
        return List.of("/api/auth/logout", "/api/account/**", "/api/admin/**");
    }
}
