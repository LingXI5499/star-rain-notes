package com.starrainnotes.account.delivery;

import com.starrainnotes.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.entity.PasswordResetEntity;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.account.utils.OneTimeTokens;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Encrypted mail jobs commit with the token; SMTP is only called by the delivery worker. */
@Component
public class MailOutbox {
    private static final Logger LOG = LoggerFactory.getLogger(MailOutbox.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final JavaMailSender sender;
    private final byte[] key;
    private final boolean enabled;
    private final AccountMapper accounts;
    private final TransactionTemplate transactions;

    public MailOutbox(JdbcTemplate jdbc, ObjectMapper json, JavaMailSender sender,
            @Value("${STAR_RAIN_MAIL_OUTBOX_KEY:}") String encodedKey,
            @Value("${star-rain.account.smtp-enabled:false}") boolean enabled,
            AccountMapper accounts, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc; this.json = json; this.sender = sender; this.enabled = enabled;
        this.accounts = accounts;
        this.transactions = new TransactionTemplate(transactionManager);
        byte[] decoded;
        try { decoded = Base64.getDecoder().decode(encodedKey); }
        catch (IllegalArgumentException invalid) { decoded = new byte[0]; }
        this.key = decoded;
    }

    public void requireReady() {
        if (key.length != 32) throw new ApiException("MAIL_OUTBOX_NOT_CONFIGURED",
                "邮件安全投递尚未配置，请联系站点管理员。", 503);
    }

    public String enqueue(String from, String recipient, String subject, String body) {
        return enqueue(new Payload(from, recipient, subject, body, null),
                subject.contains("验证码") ? 10 : subject.contains("密码重置") ? 30 : 2880);
    }

    /** All reset requests take the same encrypted insert path, independent of account existence. */
    public String enqueueResetRequest(String from, String recipient, String frontendOrigin) {
        return enqueue(new Payload(from, recipient, "星雨笔录密码重置", frontendOrigin, "RESET_REQUEST"), 30);
    }

    /** Payload encryption and persistence participate in the caller's transaction. */
    private String enqueue(Payload message, int minutes) {
        requireReady();
        String id = UUID.randomUUID().toString();
        try {
            String payload = encrypt(json.writeValueAsBytes(message));
            jdbc.update("INSERT INTO sr_mail_outbox(id,payload_cipher,expires_at,next_attempt_at) VALUES(?,?,DATE_ADD(UTC_TIMESTAMP(3),INTERVAL ? MINUTE),UTC_TIMESTAMP(3))",
                    id, payload, minutes);
            return "queued:" + id;
        } catch (ApiException failure) { throw failure; }
        catch (Exception failure) { throw new IllegalStateException("Mail enqueue failed", failure); }
    }

    @Scheduled(fixedDelayString = "${star-rain.account.mail-delivery-ms:5000}",
            initialDelayString = "${star-rain.account.mail-initial-delay-ms:5000}")
    public void deliver() {
        if (!enabled || key.length != 32) return;
        jdbc.update("DELETE FROM sr_mail_outbox WHERE expires_at<UTC_TIMESTAMP(3) LIMIT 200");
        var jobs = jdbc.query("SELECT id,payload_cipher FROM sr_mail_outbox WHERE next_attempt_at<=UTC_TIMESTAMP(3) "
                + "AND expires_at>DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 30 SECOND) AND (lease_until IS NULL OR lease_until<UTC_TIMESTAMP(3)) LIMIT 20",
                (row, index) -> new Job(row.getString(1), row.getString(2)));
        for (Job job : jobs) {
            String lease = UUID.randomUUID().toString();
            if (jdbc.update("UPDATE sr_mail_outbox SET lease_token=?,lease_until=DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 2 MINUTE) "
                    + "WHERE id=? AND payload_cipher=? AND next_attempt_at<=UTC_TIMESTAMP(3) "
                    + "AND expires_at>DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 30 SECOND) "
                    + "AND (lease_until IS NULL OR lease_until<UTC_TIMESTAMP(3))", lease, job.getId(), job.getCipher()) != 1) continue;
            try {
                Payload payload = json.readValue(decrypt(job.getCipher()), Payload.class);
                if ("RESET_REQUEST".equals(payload.getKind())) {
                    payload = prepareReset(job.getId(), lease, payload);
                    if (payload == null) continue;
                }
                var message = sender.createMimeMessage();
                var helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
                helper.setFrom(payload.getFrom()); helper.setTo(payload.getRecipient());
                helper.setSubject(payload.getSubject()); helper.setText(payload.getBody(), false);
                sender.send(message);
                jdbc.update("DELETE FROM sr_mail_outbox WHERE id=? AND lease_token=?", job.getId(), lease);
            } catch (Exception failure) {
                jdbc.update("UPDATE sr_mail_outbox SET attempts=attempts+1,lease_token=NULL,lease_until=NULL, "
                        + "next_attempt_at=DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 30 SECOND) WHERE id=? AND lease_token=?", job.getId(), lease);
                // Payloads and addresses are deliberately excluded from logs.
                LOG.warn("Mail job {} delivery failed; category={}", job.getId(), failure.getClass().getSimpleName());
            }
        }
    }

    /**
     * Materializes a token once and replaces the encrypted request in the same transaction.
     * Retries reuse the committed token; unknown addresses are removed without sending mail.
     */
    private Payload prepareReset(String jobId, String lease, Payload request) {
        return transactions.execute(status -> {
            var account = accounts.accountByEmail(request.getRecipient());
            if (account != null) account = accounts.accountByIdForUpdate(account.getId());
            if (account == null || !"ACTIVE".equals(account.getStatus())) {
                jdbc.update("DELETE FROM sr_mail_outbox WHERE id=? AND lease_token=?", jobId, lease);
                return null;
            }
            accounts.expirePendingResets(account.getId());
            String token = OneTimeTokens.create();
            PasswordResetEntity reset = new PasswordResetEntity();
            reset.setAccountId(account.getId());
            reset.setTokenHash(OneTimeTokens.hash(token));
            reset.setStatus("PENDING");
            reset.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30));
            accounts.insertReset(reset);
            Payload prepared = new Payload(request.getFrom(), request.getRecipient(), request.getSubject(),
                    "请在 30 分钟内打开以下链接重置密码：\n" + request.getBody()
                    + "/useradmin/reset-password?token=" + token + "\n如果并非本人操作，请忽略此邮件。", null);
            try {
                String cipher = encrypt(json.writeValueAsBytes(prepared));
                if (jdbc.update("UPDATE sr_mail_outbox SET payload_cipher=? WHERE id=? AND lease_token=?",
                        cipher, jobId, lease) != 1) {
                    throw new IllegalStateException("Reset mail lease lost");
                }
            } catch (Exception failure) {
                throw new IllegalStateException("Reset mail preparation failed", failure);
            }
            return prepared;
        });
    }

    private String encrypt(byte[] plaintext) throws Exception {
        byte[] iv = new byte[12]; RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(plaintext), result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length); System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(result);
    }

    private byte[] decrypt(String encoded) throws Exception {
        byte[] ciphertext = Base64.getDecoder().decode(encoded);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, ciphertext, 0, 12));
        return cipher.doFinal(ciphertext, 12, ciphertext.length - 12);
    }

    /** Compatible with already queued four-field payloads: a missing kind means a prepared message. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private static class Payload {
        private String from;
        private String recipient;
        private String subject;
        private String body;
        private String kind;
    }

    /** Worker-only encrypted job reference. */
    @Data
    @AllArgsConstructor
    private static class Job {
        private String id;
        private String cipher;
    }
}
