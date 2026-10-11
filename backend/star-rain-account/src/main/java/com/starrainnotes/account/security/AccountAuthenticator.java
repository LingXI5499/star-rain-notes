/*
 * AccountAuthenticator：账号模块的安全策略文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 * 主要协作类型：AccountPrincipal、AccountEntity、AccountMapper。
 * 阅读入口：authenticate（认证账号Authenticator）；supports（判断是否支持账号Authenticator）。
 */

package com.starrainnotes.account.security;

import com.starrainnotes.account.context.AccountPrincipal;
import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.mapper.AccountMapper;
import java.util.ArrayList;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 账号Authenticator，属于账号模块。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 */
@Component
public class AccountAuthenticator implements AuthenticationProvider {

    /**
     * 数据映射。
     * 协作类型：<code>AccountMapper</code>。
     */
    private final AccountMapper mapper;
    /**
     * 密码Encoder。
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 构造AccountAuthenticator，接收本类使用的依赖或初始化参数。
     *
     * @param mapper 数据映射；类型为 <code>AccountMapper</code>
     * @param passwordEncoder 密码Encoder；类型为 <code>PasswordEncoder</code>
     */
    public AccountAuthenticator(AccountMapper mapper, PasswordEncoder passwordEncoder) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 认证账号Authenticator。
     * 主要协作调用：<code>mapper.accountByIdentifier</code>、<code>mapper.passwordHash</code>、<code>passwordEncoder.matches</code>、<code>mapper.roleCodes</code>、<code>mapper.permissionCodes</code>。
     * 实现中的显式失败类型包括 <code>BadCredentialsException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(account == null || !"ACTIVE".equals(account.getStatus()))</code>。
     *
     * @param authentication 认证；类型为 <code>Authentication</code>
     * @return 类型为 <code>Authentication</code> 的处理结果
     */
    @Override
    public Authentication authenticate(Authentication authentication) {
        String identifier = String.valueOf(authentication.getPrincipal()).trim();
        String password = String.valueOf(authentication.getCredentials());
        AccountEntity account = mapper.accountByIdentifier(identifier);
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw new BadCredentialsException("身份信息无效");
        }
        String hash = mapper.passwordHash(account.getId());
        if (hash == null || !passwordEncoder.matches(password, hash)) {
            throw new BadCredentialsException("身份信息无效");
        }
        Set<String> roles = Set.copyOf(mapper.roleCodes(account.getId()));
        Set<String> permissions = Set.copyOf(mapper.permissionCodes(account.getId()));
        AccountPrincipal principal = new AccountPrincipal(
                account.getId(), account.getUsername(), account.getAuthVersion(), roles, permissions);
        ArrayList<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    /**
     * 判断是否支持账号Authenticator。
     * 关键处理步骤调用 <code>UsernamePasswordAuthenticationToken.class.isAssignableFrom</code>。
     *
     * @param authentication 认证；类型为 <code>Class&lt;?&gt;</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

