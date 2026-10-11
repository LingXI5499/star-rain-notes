/*
 * AccountPrincipal：账号模块的模块支持文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */

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
/**
 * 账号认证主体，属于账号模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Data
@NoArgsConstructor
public class AccountPrincipal implements Serializable {

    /**
     * 账号标识。
     */
    private Long accountId;
    /**
     * 用户名。
     */
    private String username;
    /**
     * 认证版本。
     */
    private int authVersion;
    /**
     * 角色集合。
     */
    private Set<String> roles;
    /**
     * 权限集合。
     */
    private Set<String> permissions;

    /**
     * 构造AccountPrincipal，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>Set.copyOf</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param username 用户名；类型为 <code>String</code>
     * @param authVersion 认证版本；类型为 <code>int</code>
     * @param roles 角色集合；类型为 <code>Set&lt;String&gt;</code>
     * @param permissions 权限集合；类型为 <code>Set&lt;String&gt;</code>
     */
    public AccountPrincipal(Long accountId, String username, int authVersion,
                            Set<String> roles, Set<String> permissions) {
        this.accountId = accountId;
        this.username = username;
        this.authVersion = authVersion;
        this.roles = Set.copyOf(roles);
        this.permissions = Set.copyOf(permissions);
    }
}
