package com.starrainnotes.account.service.impl;

import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.account.service.AccountThrottleService;
import com.starrainnotes.account.vo.LoginReservation;
import com.starrainnotes.common.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** MySQL row locks reserve both budgets before BCrypt, including concurrent pending attempts. */
@Service
public class AccountThrottleServiceImpl implements AccountThrottleService {
    private static final Logger LOG = LoggerFactory.getLogger(AccountThrottleServiceImpl.class);
    private final JdbcTemplate jdbc;
    private final AccountMapper accounts;
    private final byte[] key;
    private final int sourceLimit;
    private final int identityLimit;
    private final int windowSeconds;

    /** A stable private key is mandatory with production Secure cookies; development may use an ephemeral key. */
    public AccountThrottleServiceImpl(JdbcTemplate jdbc, AccountMapper accounts,
            @Value("${star-rain.account.throttle.key:}") String secret,
            @Value("${star-rain.account.throttle.source-limit:100}") int sourceLimit,
            @Value("${star-rain.account.throttle.identity-limit:10}") int identityLimit,
            @Value("${star-rain.account.throttle.window-seconds:900}") int windowSeconds,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        if (sourceLimit < 1 || identityLimit < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("Account throttle limits must be positive");
        }
        this.sourceLimit = sourceLimit;
        this.identityLimit = identityLimit;
        this.windowSeconds = windowSeconds;
        if (secret.length() < 32) {
            if (secure) {
                throw new IllegalStateException("Configure a private account throttle key of at least 32 characters");
            }
            this.key = new byte[32];
            new SecureRandom().nextBytes(this.key);
            LOG.warn("Account throttle uses an ephemeral development key; configure a stable private key before release");
        } else {
            this.key = secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    /** Locks are always acquired source first, then identity; the independent commit survives failed login. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LoginReservation reserveLogin(String ip, String identifier) {
        AccountEntity account = accounts.accountByIdentifier(identifier.trim());
        String identity = account == null ? "input:" + identifier.trim().toLowerCase(Locale.ROOT)
                : "account:" + account.getId();
        String sourceKey = hash("login:source", ip);
        String identityKey = hash("login:identity", identity);
        LocalDateTime sourceWindow = reserve(sourceKey, sourceLimit, windowSeconds, "LOGIN_RATE_LIMITED");
        LocalDateTime identityWindow = reserve(identityKey, identityLimit, windowSeconds, "LOGIN_RATE_LIMITED");
        return new LoginReservation(sourceKey, sourceWindow, identityKey, identityWindow);
    }

    /** Only successful attempts are refunded; failures remain bounded until the window expires. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginSucceeded(LoginReservation reservation) {
        release(reservation.getSourceKey(), reservation.getSourceWindow());
        release(reservation.getIdentityKey(), reservation.getIdentityWindow());
    }

    /** Separate reset budgets avoid consuming login capacity or revealing whether the email exists. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserveReset(String ip, String email) {
        reserve(hash("reset:source", ip), 30, 3600, "PASSWORD_RESET_RATE_LIMITED");
        reserve(hash("reset:email", email.toLowerCase(Locale.ROOT)), 3, 3600, "PASSWORD_RESET_RATE_LIMITED");
    }

    /** Reserves one slot under a row lock. Throwing rolls back both dimensions without oversubscription. */
    private LocalDateTime reserve(String hash, int limit, int seconds, String code) {
        jdbc.update("INSERT INTO sr_login_throttle(throttle_key,window_started_at,attempts) "
                + "VALUES(?,UTC_TIMESTAMP(3),0) ON DUPLICATE KEY UPDATE throttle_key=throttle_key", hash);
        return jdbc.queryForObject("SELECT window_started_at,attempts,UTC_TIMESTAMP(3) FROM sr_login_throttle "
                + "WHERE throttle_key=? FOR UPDATE", (row, index) -> {
                    // DATETIME represents a UTC wall clock, not a JVM-zone Timestamp instant.
                    LocalDateTime window = row.getObject(1, LocalDateTime.class);
                    int attempts = row.getInt(2);
                    LocalDateTime now = row.getObject(3, LocalDateTime.class);
                    if (!window.plusSeconds(seconds).isAfter(now)) {
                        window = now;
                        attempts = 0;
                    }
                    if (attempts >= limit) {
                        throw new ApiException(code, "请求过于频繁，请稍后重试（最长等待 " + seconds + " 秒）", 429);
                    }
                    jdbc.update("UPDATE sr_login_throttle SET window_started_at=?,attempts=? WHERE throttle_key=?",
                            window, attempts + 1, hash);
                    return window;
                }, hash);
    }

    /** A late success must not refund a reservation from a different window. */
    private void release(String hash, LocalDateTime window) {
        jdbc.update("UPDATE sr_login_throttle SET attempts=GREATEST(0,attempts-1) "
                + "WHERE throttle_key=? AND window_started_at=?", hash, window);
    }

    /** HMAC keys conceal arbitrary login input, email and IP from database diagnostics. */
    private String hash(String scope, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((scope + "\n" + value).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("Account throttle HMAC unavailable", failure);
        }
    }

    /** Bounded cleanup of expired counters; permanent audit history is never touched. */
    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    public void cleanup() {
        jdbc.update("DELETE FROM sr_login_throttle WHERE window_started_at<DATE_SUB(UTC_TIMESTAMP(3),INTERVAL 1 DAY) LIMIT 1000");
    }
}
