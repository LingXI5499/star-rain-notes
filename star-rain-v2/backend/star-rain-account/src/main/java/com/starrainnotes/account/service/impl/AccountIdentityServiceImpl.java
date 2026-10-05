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

@Service
public class AccountIdentityServiceImpl implements AccountIdentityService, CurrentActorApi,
        AccountReferenceApi, PermissionQueryApi {

    private final AccountMapper mapper;

    public AccountIdentityServiceImpl(AccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AccountPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new ApiException("UNAUTHORIZED", "请先登录", 401);
        }
        return principal;
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentAccountVO currentView() {
        return view(principal().getAccountId());
    }

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

    @Override
    public CurrentActor current() {
        AccountPrincipal principal = principal();
        return new CurrentActor(principal.getAccountId(), principal.getRoles(), principal.getPermissions());
    }

    @Override
    public Optional<CurrentActor> currentOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            return Optional.of(new CurrentActor(principal.getAccountId(), principal.getRoles(),
                    principal.getPermissions()));
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long accountId) {
        return accountId != null && mapper.accountById(accountId) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long accountId) {
        AccountEntity account = accountId == null ? null : mapper.accountById(accountId);
        return account != null && "ACTIVE".equals(account.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public String displayName(Long accountId) {
        AccountEntity account = accountId == null ? null : mapper.accountById(accountId);
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("ACCOUNT_NOT_FOUND", "账户不可用", 404);
        }
        return account.getDisplayName();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(Long accountId, String permissionCode) {
        return isActive(accountId) && mapper.permissionCodes(accountId).contains(permissionCode);
    }
}
