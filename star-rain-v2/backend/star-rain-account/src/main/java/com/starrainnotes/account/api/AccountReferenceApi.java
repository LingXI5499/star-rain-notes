package com.starrainnotes.account.api;

/*
 * 其他模块检查账户逻辑引用的唯一入口。
 */
public interface AccountReferenceApi {

    boolean exists(Long accountId);

    boolean isActive(Long accountId);
}

