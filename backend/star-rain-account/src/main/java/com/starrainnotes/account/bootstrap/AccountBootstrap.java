/*
 * AccountBootstrap：账号模块的模块支持文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：AccountAuthService。
 * 阅读入口：run（运行账号初始化）。
 */

package com.starrainnotes.account.bootstrap;

import com.starrainnotes.account.service.AccountAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 账号初始化，属于账号模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
public class AccountBootstrap implements ApplicationRunner {

    /**
     * 业务服务。
     * 协作类型：<code>AccountAuthService</code>。
     */
    private final AccountAuthService service;
    /**
     * 已启用。
     */
    private final boolean enabled;
    /**
     * 用户名。
     */
    private final String username;
    /**
     * 邮箱。
     */
    private final String email;
    /**
     * 密码参数；使用和保存方式由对应的密码处理流程决定。
     */
    private final String password;

    /**
     * 构造AccountBootstrap，接收本类使用的依赖或初始化参数。
     *
     * @param service 业务服务；类型为 <code>AccountAuthService</code>
     * @param enabled 已启用；类型为 <code>boolean</code>
     * @param username 用户名；类型为 <code>String</code>
     * @param email 邮箱；类型为 <code>String</code>
     * @param password 密码参数；使用和保存方式由对应的密码处理流程决定；类型为 <code>String</code>
     */
    public AccountBootstrap(AccountAuthService service,
                            @Value("${STAR_RAIN_BOOTSTRAP_ADMIN_ENABLED:false}") boolean enabled,
                            @Value("${STAR_RAIN_BOOTSTRAP_ADMIN_USERNAME:}") String username,
                            @Value("${STAR_RAIN_BOOTSTRAP_ADMIN_EMAIL:}") String email,
                            @Value("${STAR_RAIN_BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
        this.service = service;
        this.enabled = enabled;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    /**
     * 运行账号初始化。
     * 主要协作调用：<code>service.bootstrapSuperAdmin</code>。
     *
     * @param args args；类型为 <code>ApplicationArguments</code>
     */
    @Override
    public void run(ApplicationArguments args) {
        service.bootstrapSuperAdmin(enabled, username, email, password);
    }
}
