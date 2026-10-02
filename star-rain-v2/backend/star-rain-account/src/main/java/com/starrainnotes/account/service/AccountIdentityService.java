package com.starrainnotes.account.service;

import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.vo.CurrentAccountVO;
import com.starrainnotes.account.api.AccountReferenceApi;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.account.api.PermissionQueryApi;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.common.ApiException;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountIdentityService implements CurrentActorApi, AccountReferenceApi, PermissionQueryApi {

    private final AccountMapper mapper;

    public AccountIdentityService(AccountMapper mapper) {
        this.mapper = mapper;
    }

    public AccountPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new ApiException("UNAUTHORIZED", "请先登录", 401);
        }
        return principal;
    }

    @Transactional(readOnly = true)
    public CurrentAccountVO currentView() {
        return view(principal().accountId());
    }

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
        return new CurrentActor(principal.accountId(), principal.roles(), principal.permissions());
    }

    @Override
    public Optional<CurrentActor> currentOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            return Optional.of(new CurrentActor(principal.accountId(), principal.roles(),
                    principal.permissions()));
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
    public boolean hasPermission(Long accountId, String permissionCode) {
        return isActive(accountId) && mapper.permissionCodes(accountId).contains(permissionCode);
    }
}

