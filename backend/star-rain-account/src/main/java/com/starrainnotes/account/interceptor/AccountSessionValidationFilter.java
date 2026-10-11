/*
 * AccountSessionValidationFilter：账号模块的模块支持文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：AccountPrincipal、AccountEntity、AccountMapper。
 * 阅读入口：doFilterInternal（do筛选Internal处理入口）。
 */

package com.starrainnotes.account.interceptor;

import com.starrainnotes.account.context.AccountPrincipal;
import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.mapper.AccountMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 账号会话校验筛选，属于账号模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
public class AccountSessionValidationFilter extends OncePerRequestFilter {

    /**
     * 数据映射。
     * 协作类型：<code>AccountMapper</code>。
     */
    private final AccountMapper mapper;

    /**
     * 构造AccountSessionValidationFilter，接收本类使用的依赖或初始化参数。
     *
     * @param mapper 数据映射；类型为 <code>AccountMapper</code>
     */
    public AccountSessionValidationFilter(AccountMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * do筛选Internal处理入口。
     * 主要协作调用：<code>mapper.accountById</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>HttpServletRequest</code>
     * @param response 响应；类型为 <code>HttpServletResponse</code>
     * @param chain chain；类型为 <code>FilterChain</code>
     * @throws ServletException 底层操作失败或输入不满足本方法要求时抛出
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            AccountEntity account = mapper.accountById(principal.getAccountId());
            if (account == null || !"ACTIVE".equals(account.getStatus())
                    || account.getAuthVersion() == null || account.getAuthVersion() != principal.getAuthVersion()) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }
            }
        }
        chain.doFilter(request, response);
    }
}

