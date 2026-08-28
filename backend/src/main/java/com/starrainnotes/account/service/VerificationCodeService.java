package com.starrainnotes.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.account.entity.EmailVerificationChallenge;
import com.starrainnotes.account.mapper.EmailVerificationChallengeMapper;
import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

/**
 * Email verification codes: 6-digit, 10 min expiry, 5 attempts, email 60s and
 * hourly limits, per-IP hourly limit. Only digests are persisted; a new code
 * invalidates prior unconsumed codes and a successful verify consumes the code.
 */
@Service
public class VerificationCodeService {

    static final int CODE_TTL_MINUTES = 10;
    static final int MAX_ATTEMPTS = 5;
    static final long EMAIL_RATE_MS = 60_000L;
    static final int EMAIL_HOURLY_MAX = 5;
    static final int IP_HOURLY_MAX = 10;

    private final EmailVerificationChallengeMapper mapper;
    private final MailGateway mailGateway;

    public VerificationCodeService(EmailVerificationChallengeMapper mapper, MailGateway mailGateway) {
        this.mapper = mapper;
        this.mailGateway = mailGateway;
    }

    @Transactional
    public void issue(String email, String purpose, Long invitationId, String ip) {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        checkRateLimits(email, purpose, ip, now);

        EmailVerificationChallenge latest = latestUnconsumed(email, purpose);
        if (latest != null && latest.getCreatedAt().plusSeconds(EMAIL_RATE_MS / 1000).isAfter(now)) {
            throw fail("VERIFICATION_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                    "Verification rate limited", "Please wait before requesting another code.");
        }

        String code = random6();
        EmailVerificationChallenge challenge = new EmailVerificationChallenge();
        challenge.setEmail(email);
        challenge.setPurpose(purpose);
        challenge.setInvitationId(invitationId);
        challenge.setCodeHash(sha256(code));
        challenge.setAttemptCount(0);
        challenge.setExpiresAt(now.plusMinutes(CODE_TTL_MINUTES));
        challenge.setRequestIpHash(ip == null ? null : sha256(ip));
        challenge.setCreatedAt(now);
        // invalidate prior codes
        if (latest != null) {
            latest.setConsumedAt(now);
            mapper.updateById(latest);
        }
        mapper.insert(challenge);
        mailGateway.sendVerificationCode(email, purpose, code);
    }

    public void verify(String email, String purpose, String code, Long invitationId) {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        EmailVerificationChallenge challenge = latestUnconsumed(email, purpose, invitationId);
        if (challenge == null) {
            throw fail("VERIFICATION_CODE_INVALID", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Verification code invalid", "No active verification code.");
        }
        if (challenge.getExpiresAt().isBefore(now)) {
            throw fail("VERIFICATION_CODE_EXPIRED", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Verification code expired", "The code has expired.");
        }
        if (challenge.getAttemptCount() >= MAX_ATTEMPTS) {
            throw fail("VERIFICATION_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts", "Too many attempts; request a new code.");
        }
        if (!sha256(code).equals(challenge.getCodeHash())) {
            challenge.setAttemptCount(challenge.getAttemptCount() + 1);
            mapper.updateById(challenge);
            throw fail("VERIFICATION_CODE_INVALID", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Verification code invalid", "The code does not match.");
        }
        challenge.setConsumedAt(now);
        mapper.updateById(challenge);
    }

    public EmailVerificationChallenge latestUnconsumed(String email, String purpose) {
        return latestUnconsumed(email, purpose, null);
    }

    private EmailVerificationChallenge latestUnconsumed(String email, String purpose, Long invitationId) {
        LambdaQueryWrapper<EmailVerificationChallenge> query = new LambdaQueryWrapper<EmailVerificationChallenge>()
                .eq(EmailVerificationChallenge::getEmail, email)
                .eq(EmailVerificationChallenge::getPurpose, purpose)
                .eq(invitationId != null, EmailVerificationChallenge::getInvitationId, invitationId)
                .isNull(EmailVerificationChallenge::getConsumedAt);
        List<EmailVerificationChallenge> list = mapper.selectList(query
                .orderByDesc(EmailVerificationChallenge::getCreatedAt)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private void checkRateLimits(String email, String purpose, String ip, LocalDateTime now) {
        LocalDateTime hourAgo = now.minusHours(1);
        Long emailHour = count(email, purpose, hourAgo, null);
        if (emailHour != null && emailHour >= EMAIL_HOURLY_MAX) {
            throw fail("VERIFICATION_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                    "Verification rate limited", "Hourly limit reached for this email.");
        }
        if (ip != null) {
            Long ipHour = count(null, null, hourAgo, sha256(ip));
            if (ipHour != null && ipHour >= IP_HOURLY_MAX) {
                throw fail("VERIFICATION_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                        "Verification rate limited", "Hourly limit reached for this network.");
            }
        }
    }

    private Long count(String email, String purpose, LocalDateTime since, String ipHash) {
        LambdaQueryWrapper<EmailVerificationChallenge> w = new LambdaQueryWrapper<>();
        if (email != null) w.eq(EmailVerificationChallenge::getEmail, email);
        if (purpose != null) w.eq(EmailVerificationChallenge::getPurpose, purpose);
        if (ipHash != null) w.eq(EmailVerificationChallenge::getRequestIpHash, ipHash);
        w.ge(EmailVerificationChallenge::getCreatedAt, since);
        return mapper.selectCount(w);
    }

    public static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("hash unavailable", ex);
        }
    }

    private static String random6() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1_000_000));
    }

    static ApiException fail(String code, HttpStatus status, String title, String detail) {
        return new ApiException(status, code, title, detail);
    }

    static DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
}
