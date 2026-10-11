/*
 * AccountIdentityServiceImpl：账号模块的业务实现文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：AccountEntity、CurrentAccountVO、AccountReferenceApi、CurrentActorApi、PermissionQueryApi、AccountMapper、AccountPrincipal。
 * 阅读入口：principal（认证主体处理入口）；currentView（当前View处理入口）；view（view处理入口）；current（当前处理入口）。
 */

package com.starrainnotes.account.service.impl;

import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.vo.CurrentAccountVO;
import com.starrainnotes.account.api.AccountReferenceApi;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.account.api.PermissionQueryApi;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.account.context.AccountPrincipal;
import com.starrainnotes.account.service.AccountIdentityService;
import com.starrainnotes.common.exception.ApiException;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账号身份业务服务实现，属于账号模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class AccountIdentityServiceImpl implements AccountIdentityService, CurrentActorApi,
        AccountReferenceApi, PermissionQueryApi {

    /**
     * 数据映射。
     * 协作类型：<code>AccountMapper</code>。
     */
    private final AccountMapper mapper;

    /**
     * 构造AccountIdentityServiceImpl，接收本类使用的依赖或初始化参数。
     *
     * @param mapper 数据映射；类型为 <code>AccountMapper</code>
     */
    public AccountIdentityServiceImpl(AccountMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 认证主体处理入口。
     * 关键处理步骤调用 <code>SecurityContextHolder.getContext</code>、<code>authentication.getPrincipal</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     * @return 类型为 <code>AccountPrincipal</code> 的处理结果
     */
    @Override
    public AccountPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new ApiException("UNAUTHORIZED", "请先登录", 401);
        }
        return principal;
    }

    /**
     * 当前View处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>view</code>、<code>principal</code>。
     * @return 类型为 <code>CurrentAccountVO</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public CurrentAccountVO currentView() {
        return view(principal().getAccountId());
    }

    /**
     * view处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.accountById</code>、<code>mapper.roleCodes</code>、<code>mapper.permissionCodes</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @return 类型为 <code>CurrentAccountVO</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public CurrentAccountVO view(long accountId) {
        AccountEntity account = mapper.accountById(accountId);
        if (account == null) {
            throw new ApiException("ACCOUNT_NOT_FOUND", "账户不存在", 404);
        }
        return new CurrentAccountVO(String.valueOf(account.getId()), account.getUsername(), account.getEmail(),
                account.getDisplayName(), account.getStatus(), Set.copyOf(mapper.roleCodes(account.getId())),
                Set.copyOf(mapper.permissionCodes(account.getId())), account.getEmailVerifiedAt() != null);
    }

    /**
     * 当前处理入口。
     * 关键处理步骤调用 <code>principal</code>、<code>principal.getAccountId</code>、<code>principal.getRoles</code>、<code>principal.getPermissions</code>。
     * @return 类型为 <code>CurrentActor</code> 的处理结果
     */
    @Override
    public CurrentActor current() {
        AccountPrincipal principal = principal();
        return new CurrentActor(principal.getAccountId(), principal.getRoles(), principal.getPermissions());
    }

    /**
     * 当前Optional处理入口。
     * 关键处理步骤调用 <code>SecurityContextHolder.getContext</code>、<code>authentication.getPrincipal</code>、<code>Optional.of</code>、<code>principal.getAccountId</code>。
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    public Optional<CurrentActor> currentOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            return Optional.of(new CurrentActor(principal.getAccountId(), principal.getRoles(),
                    principal.getPermissions()));
        }
        return Optional.empty();
    }

    /**
     * 判断是否存在账号身份。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.accountById</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long accountId) {
        return accountId != null && mapper.accountById(accountId) != null;
    }

    /**
     * 判断是否有效。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.accountById</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long accountId) {
        AccountEntity account = accountId == null ? null : mapper.accountById(accountId);
        return account != null && "ACTIVE".equals(account.getStatus());
    }

    /**
     * 显示名称处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.accountById</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public String displayName(Long accountId) {
        AccountEntity account = accountId == null ? null : mapper.accountById(accountId);
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("ACCOUNT_NOT_FOUND", "账户不可用", 404);
        }
        return account.getDisplayName();
    }

    /**
     * 判断是否具备权限。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.permissionCodes</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param permissionCode 权限的业务代码；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(Long accountId, String permissionCode) {
        return isActive(accountId) && mapper.permissionCodes(accountId).contains(permissionCode);
    }
}
