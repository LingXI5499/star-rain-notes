package com.starrainnotes.account.delivery;

import com.starrainnotes.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public MailOutbox(JdbcTemplate jdbc, ObjectMapper json, JavaMailSender sender,
            @Value("${STAR_RAIN_MAIL_OUTBOX_KEY:}") String encodedKey,
            @Value("${star-rain.account.smtp-enabled:false}") boolean enabled) {
        this.jdbc = jdbc; this.json = json; this.sender = sender; this.enabled = enabled;
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
        requireReady();
        String id = UUID.randomUUID().toString();
        int minutes = subject.contains("验证码") ? 10 : subject.contains("密码重置") ? 30 : 2880;
        try {
            String payload = encrypt(json.writeValueAsBytes(new Payload(from, recipient, subject, body)));
            jdbc.update("INSERT INTO sr_mail_outbox(id,payload_cipher,expires_at,next_attempt_at) VALUES(?,?,DATE_ADD(UTC_TIMESTAMP(3),INTERVAL ? MINUTE),UTC_TIMESTAMP(3))",
                    id, payload, minutes);
            return "queued:" + id;
        } catch (ApiException failure) { throw failure; }
        catch (Exception failure) { throw new IllegalStateException("Mail enqueue failed", failure); }
    }

    @Scheduled(fixedDelayString = "${star-rain.account.mail-delivery-ms:5000}", initialDelay = 5000)
    public void deliver() {
        if (!enabled || key.length != 32) return;
        jdbc.update("DELETE FROM sr_mail_outbox WHERE expires_at<UTC_TIMESTAMP(3) LIMIT 200");
        var jobs = jdbc.query("SELECT id,payload_cipher FROM sr_mail_outbox WHERE next_attempt_at<=UTC_TIMESTAMP(3) "
                + "AND expires_at>DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 30 SECOND) AND (lease_until IS NULL OR lease_until<UTC_TIMESTAMP(3)) LIMIT 20",
                (row, index) -> new Job(row.getString(1), row.getString(2)));
        for (Job job : jobs) {
            String lease = UUID.randomUUID().toString();
            if (jdbc.update("UPDATE sr_mail_outbox SET lease_token=?,lease_until=DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 2 MINUTE) "
                    + "WHERE id=? AND (lease_until IS NULL OR lease_until<UTC_TIMESTAMP(3))", lease, job.id()) != 1) continue;
            try {
                Payload payload = json.readValue(decrypt(job.cipher()), Payload.class);
                var message = sender.createMimeMessage();
                var helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
                helper.setFrom(payload.from()); helper.setTo(payload.recipient());
                helper.setSubject(payload.subject()); helper.setText(payload.body(), false);
                sender.send(message);
                jdbc.update("DELETE FROM sr_mail_outbox WHERE id=? AND lease_token=?", job.id(), lease);
            } catch (Exception failure) {
                jdbc.update("UPDATE sr_mail_outbox SET attempts=attempts+1,lease_token=NULL,lease_until=NULL, "
                        + "next_attempt_at=DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 30 SECOND) WHERE id=? AND lease_token=?", job.id(), lease);
                // Payloads and addresses are deliberately excluded from logs.
                LOG.warn("Mail job {} delivery failed; category={}", job.id(), failure.getClass().getSimpleName());
            }
        }
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

    private record Payload(String from, String recipient, String subject, String body) { }
    private record Job(String id, String cipher) { }
}
