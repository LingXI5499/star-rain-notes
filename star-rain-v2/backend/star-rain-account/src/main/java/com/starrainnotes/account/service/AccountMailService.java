package com.starrainnotes.account.service;

import com.starrainnotes.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import jakarta.mail.MessagingException;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.net.URI;
import java.util.Date;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AccountMailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountMailService.class);

    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from;
    private final String frontendOrigin;

    public AccountMailService(JavaMailSender sender,
                              @Value("${star-rain.account.smtp-enabled:false}") boolean enabled,
                              @Value("${star-rain.account.mail-from:}") String from,
                              @Value("${star-rain.account.frontend-origin}") String frontendOrigin) {
        this.sender = sender;
        this.enabled = enabled;
        this.from = from;
        this.frontendOrigin = frontendOrigin.replaceAll("/+$", "");
    }

    public void requireDelivery() {
        if (!enabled || from.isBlank()) {
            throw new ApiException("MAIL_NOT_CONFIGURED", "邮件服务尚未配置", 503);
        }
    }

    public String resetUrl(String token) {
        return frontendOrigin + "/reset-password?token=" + token;
    }

    public String invitationUrl(String token) {
        return frontendOrigin + "/invitation/accept?token=" + token;
    }

    public void sendReset(String email, String token) {
        requireDelivery();
        send(email, "星雨笔录密码重置", "请在 30 分钟内打开以下链接重置密码：\n"
                + resetUrl(token) + "\n如果并非本人操作，请忽略此邮件。");
    }

    public void sendEmailVerificationCode(String email, String code) {
        requireDelivery();
        send(email, "星雨笔录邮箱验证码", "你的邮箱验证码是：" + code
                + "，10 分钟内有效。\n请在星雨笔录页面输入验证码以完成邮箱验证。\n如果并非本人操作，请忽略此邮件。");
    }

    public String sendInvitation(String email, String token) {
        requireDelivery();
        String body = "你的星雨笔录账户收到一条管理员邀请。\n"
                + "请在 48 小时内使用本邮箱对应的账户登录星雨笔录，在“我的账户”的“待处理邀请”中查看并决定是否接受。\n"
                + "接受邀请后将获得管理员身份。非预期邀请可忽略，并联系站点管理员。";
        String host = URI.create(frontendOrigin).getHost();
        // 本机回环地址不能作为其他设备可访问的邮件链接。
        if (host != null && !Set.of("localhost", "127.0.0.1", "[::1]", "::1").contains(host)) {
            body += "\n也可通过邀请链接进入：\n" + invitationUrl(token);
        }
        return send(email, "星雨笔录：你有一条待处理的管理员邀请", body);
    }

    private String send(String email, String subject, String body) {
        try {
            MimeMessage message = sender.createMimeMessage();
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(subject, "UTF-8");
            message.setText(body, "UTF-8");
            message.setSentDate(new Date());
            sender.send(message);
            return message.getMessageID();
        } catch (MailException | MessagingException exception) {
            LOGGER.warn("Mail submission failed; category={}", exception.getClass().getSimpleName());
            throw new ApiException("MAIL_DELIVERY_FAILED", "邮件发送失败，请稍后重试", 503);
        }
    }
}
