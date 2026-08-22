package com.starrainnotes.auth.service;

import com.starrainnotes.auth.entity.AdminUser;
import com.starrainnotes.auth.mapper.AdminUserMapper;
import com.starrainnotes.common.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * V1 setup flow (04-api-design.md §7): create the single administrator.
 *
 * <p>Requirements before creating the admin:</p>
 * <ol>
 *   <li>admin_user is empty</li>
 *   <li>APP_SETUP_TOKEN is configured</li>
 *   <li>X-Setup-Token matches APP_SETUP_TOKEN in constant time</li>
 *   <li>CSRF passed (enforced by the security filter chain)</li>
 * </ol>
 *
 * <p>After the first admin is created, setup is permanently unavailable.
 * No default administrator is ever created.</p>
 */
@Service
public class SetupService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final String setupToken;

    public SetupService(AdminUserMapper adminUserMapper,
                        PasswordEncoder passwordEncoder,
                        @Value("${app.setup-token:}") String setupToken) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.setupToken = setupToken;
    }

    public boolean isSetupRequired() {
        return adminUserMapper.selectCount(null) == 0;
    }

    public void createAdmin(String username, String rawPassword, String providedToken) {
        if (!isSetupRequired()) {
            throw new ApiException(HttpStatus.CONFLICT, "SETUP_ALREADY_COMPLETED",
                    "Setup already completed",
                    "An administrator already exists; setup is no longer available.");
        }
        if (!StringUtils.hasText(setupToken)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SETUP_TOKEN_NOT_CONFIGURED",
                    "Setup token not configured",
                    "APP_SETUP_TOKEN is not configured on the server; setup cannot proceed.");
        }
        if (!constantTimeEquals(setupToken, providedToken)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SETUP_TOKEN_MISMATCH",
                    "Invalid setup token",
                    "The provided X-Setup-Token does not match APP_SETUP_TOKEN.");
        }
        AdminUser admin = new AdminUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        adminUserMapper.insert(admin);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = (actual == null ? "" : actual).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}
