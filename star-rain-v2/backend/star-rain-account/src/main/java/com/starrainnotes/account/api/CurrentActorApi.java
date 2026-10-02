package com.starrainnotes.account.api;

import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 其他模块获取当前认证主体的唯一入口。
 */
public interface CurrentActorApi {

    CurrentActor current();

    Optional<CurrentActor> currentOptional();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CurrentActor {

        private Long accountId;
        private Set<String> roles;
        private Set<String> permissions;
    }
}
