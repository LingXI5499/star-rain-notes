package com.starrainnotes.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.account.config.AccountProperties;
import com.starrainnotes.account.entity.AccountUser;
import com.starrainnotes.account.mapper.AccountUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountQueryService {
    private final AccountUserMapper userMapper;
    private final AccountProperties properties;

    public AccountQueryService(AccountUserMapper userMapper, AccountProperties properties) {
        this.userMapper = userMapper;
        this.properties = properties;
    }

    public boolean superAdminActivated() {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<AccountUser>()
                .eq(AccountUser::getRole, AccountService.SUPER_ADMIN)
                .eq(AccountUser::getAccountStatus, AccountService.ACTIVE));
        return count != null && count > 0;
    }

    public String configuredSuperAdminEmail() {
        return properties.getSuperAdminEmail();
    }

    public List<AccountUser> listUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<AccountUser>()
                .orderByDesc(AccountUser::getId));
    }

    public boolean isSessionValid(Long accountId, int authVersion) {
        AccountUser user = accountId == null ? null : userMapper.selectOne(
                new LambdaQueryWrapper<AccountUser>()
                        .select(AccountUser::getAccountStatus, AccountUser::getAuthVersion)
                        .eq(AccountUser::getId, accountId)
                        .last("LIMIT 1"));
        return user != null && AccountService.ACTIVE.equals(user.getAccountStatus())
                && user.getAuthVersion() != null && user.getAuthVersion() == authVersion;
    }
}
