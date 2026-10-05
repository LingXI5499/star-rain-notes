package com.starrainnotes.account.service;

import com.starrainnotes.account.api.AccountReferenceApi;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.account.api.PermissionQueryApi;
import com.starrainnotes.account.context.AccountPrincipal;
import com.starrainnotes.account.vo.CurrentAccountVO;

// 账户身份入口：当前登录主体解析与账户视图读取。
public interface AccountIdentityService extends CurrentActorApi, AccountReferenceApi, PermissionQueryApi {

    AccountPrincipal principal();

    CurrentAccountVO currentView();

    CurrentAccountVO view(long accountId);
}
