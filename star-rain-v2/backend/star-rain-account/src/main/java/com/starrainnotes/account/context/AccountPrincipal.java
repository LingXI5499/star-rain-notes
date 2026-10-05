package com.starrainnotes.account.context;

import java.io.Serializable;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 当前登录主体。
 *
 * 刻意不加 @Builder：全参构造器会用 Set.copyOf 把角色与权限规范化成不可变集合（这是有意的，
 * 下游到处直接 getRoles().contains(...)，不能容忍 null），而 Lombok 的 builder 会调用这个构造器。
 * 于是 builder().accountId(x).build() 这种「只设部分字段」的用法会直接 NPE —— 一个看起来能用、
 * 一点就炸的 API。全仓没有一处使用它，所以直接删掉，只保留构造器与 JavaBean 访问器。
 */
@Data
@NoArgsConstructor
public class AccountPrincipal implements Serializable {

    private Long accountId;
    private String username;
    private int authVersion;
    private Set<String> roles;
    private Set<String> permissions;

    public AccountPrincipal(Long accountId, String username, int authVersion,
                            Set<String> roles, Set<String> permissions) {
        this.accountId = accountId;
        this.username = username;
        this.authVersion = authVersion;
        this.roles = Set.copyOf(roles);
        this.permissions = Set.copyOf(permissions);
    }
}
