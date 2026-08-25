package com.starrainnotes.account.service;

import com.starrainnotes.account.config.AccountProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * SMTP gateway via the 163 authorized-code mailbox. Credentials are read only
 * from env-backed config; never logged or echoed to clients.
 */
@Service
public class SmtpMailGateway implements MailGateway {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailGateway.class);

    private final AccountProperties props;
    private final JavaMailSender sender;

    public SmtpMailGateway(AccountProperties props) {
        this.props = props;
        this.sender = buildSender(props.getMail());
    }

    private static JavaMailSender buildSender(AccountProperties.Mail mail) {
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(mail.getHost());
        impl.setPort(mail.getPort());
        if (mail.getUsername() != null) impl.setUsername(mail.getUsername());
        if (mail.getAuthCode() != null) impl.setPassword(mail.getAuthCode());
        impl.setDefaultEncoding("UTF-8");
        Properties javaMail = impl.getJavaMailProperties();
        javaMail.put("mail.smtp.auth", "true");
        javaMail.put("mail.smtp.connectiontimeout", "5000");
        javaMail.put("mail.smtp.timeout", "5000");
        javaMail.put("mail.smtp.writetimeout", "5000");
        if (mail.isSslEnabled()) {
            javaMail.put("mail.smtp.socketFactory.port", String.valueOf(mail.getPort()));
            javaMail.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            javaMail.put("mail.smtp.ssl.enable", "true");
        }
        return impl;
    }

    @Override
    public void sendVerificationCode(String to, String purpose, String code) {
        AccountProperties.Mail mail = props.getMail();
        if (mail.getHost() == null || mail.getHost().isBlank() || mail.getAuthCode() == null || mail.getAuthCode().isBlank()) {
            log.warn("SMTP not configured; skipping verification-code email to {}", mask(to));
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mail.getFrom() != null ? mail.getFrom() : mail.getUsername());
            message.setTo(to);
            message.setSubject(subjectFor(purpose));
            message.setText("你的验证码是：" + code + "，10 分钟内有效。");
            sender.send(message);
        } catch (Exception ex) {
            log.error("SMTP send failed to {}", mask(to), ex);
            throw new IllegalStateException("MAIL_SEND_FAILED");
        }
    }

    @Override
    public void sendInvitationLink(String to, String link) {
        AccountProperties.Mail mail = props.getMail();
        if (mail.getHost() == null || mail.getHost().isBlank() || mail.getAuthCode() == null || mail.getAuthCode().isBlank()) {
            log.warn("SMTP not configured; skipping invitation email to {}", mask(to));
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mail.getFrom() != null ? mail.getFrom() : mail.getUsername());
            message.setTo(to);
            message.setSubject("邀请你协作维护星雨笔录");
            message.setText("点击以下链接完成邀请注册（72 小时内有效）：\n" + link);
            sender.send(message);
        } catch (Exception ex) {
            log.error("SMTP send failed to {}", mask(to), ex);
            throw new IllegalStateException("MAIL_SEND_FAILED");
        }
    }

    private String subjectFor(String purpose) {
        return switch (purpose) {
            case "SUPER_ADMIN_ACTIVATION" -> "星雨笔录｜超级管理员激活验证码";
            case "ADMIN_REGISTRATION" -> "星雨笔录｜管理员邀请注册验证码";
            default -> "星雨笔录｜密码重置验证码";
        };
    }

    private static String mask(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        return at <= 0 ? "***" : email.substring(0, 2) + "***" + email.substring(at);
    }
}