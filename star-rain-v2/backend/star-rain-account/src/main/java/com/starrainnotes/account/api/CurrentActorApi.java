package com.starrainnotes.account.api;

import java.util.Optional;
import java.util.Set;

/**
 * 其他模块获取当前认证主体的唯一入口。
 */
public interface CurrentActorApi {

    CurrentActor current();

    Optional<CurrentActor> currentOptional();

    record CurrentActor(Long accountId, Set<String> roles, Set<String> permissions) {
    }
}

