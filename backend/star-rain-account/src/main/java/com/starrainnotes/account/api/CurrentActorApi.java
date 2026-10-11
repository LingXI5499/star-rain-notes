/*
 * CurrentActorApi：账号模块的跨模块接口文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：current（当前处理入口）；currentOptional（当前Optional处理入口）。
 */

package com.starrainnotes.account.api;

import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 其他模块获取当前认证主体的唯一入口。
 */
/**
 * 当前操作者接口，属于账号模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface CurrentActorApi {

    /**
     * 当前处理入口。
     * @return 类型为 <code>CurrentActor</code> 的处理结果
     */
    CurrentActor current();

    /**
     * 当前Optional处理入口。
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    Optional<CurrentActor> currentOptional();

    /**
     * 当前操作者，属于账号模块。
     * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CurrentActor {

        /**
         * 账号标识。
         */
        private Long accountId;
        /**
         * 角色集合。
         */
        private Set<String> roles;
        /**
         * 权限集合。
         */
        private Set<String> permissions;
    }
}
