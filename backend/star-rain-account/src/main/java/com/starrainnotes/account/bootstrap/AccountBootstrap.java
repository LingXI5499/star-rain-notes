package com.starrainnotes.account.bootstrap;

import com.starrainnotes.account.service.AccountAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AccountBootstrap implements ApplicationRunner {

    private final AccountAuthService service;
    private final boolean enabled;
    private final String username;
    private final String email;
    private final String password;

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

    @Override
    public void run(ApplicationArguments args) {
        service.bootstrapSuperAdmin(enabled, username, email, password);
    }
}
