/*
 * AccountMailServiceImpl：账号模块的业务实现文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：AccountMailService、ApiException。
 * 阅读入口：requireDelivery（检查所需Delivery）；resetUrl（重置地址）；invitationUrl（邀请地址处理入口）；sendReset（发送Reset）。
 */

package com.starrainnotes.account.service.impl;
import com.starrainnotes.account.delivery.MailOutbox;

import com.starrainnotes.account.service.AccountMailService;
import com.starrainnotes.common.exception.ApiException;
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

/**
 * 账号邮件业务服务实现，属于账号模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class AccountMailServiceImpl implements AccountMailService {
    /**
     * 当前类的日志记录器；日志参数不能包含密码或会话令牌。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountMailServiceImpl.class);

    /**
     * sender。
     */
    private final JavaMailSender sender;
    private final MailOutbox outbox;
    /**
     * 已启用。
     */
    private final boolean enabled;
    /**
     * 来源。
     */
    private final String from;
    /**
     * frontend来源。
     */
    private final String frontendOrigin;

    /**
     * 构造AccountMailServiceImpl，接收本类使用的依赖或初始化参数。
     * 主要协作调用：<code>frontendOrigin.replaceAll</code>。
     *
     * @param sender sender；类型为 <code>JavaMailSender</code>
     * @param enabled 已启用；类型为 <code>boolean</code>
     * @param from 来源；类型为 <code>String</code>
     * @param frontendOrigin frontend来源；类型为 <code>String</code>
     */
    public AccountMailServiceImpl(JavaMailSender sender, MailOutbox outbox,
                              @Value("${star-rain.account.smtp-enabled:false}") boolean enabled,
                              @Value("${star-rain.account.mail-from:}") String from,
                              @Value("${star-rain.account.frontend-origin}") String frontendOrigin) {
        this.sender = sender;
        this.outbox = outbox;
        this.enabled = enabled;
        this.from = from;
        this.frontendOrigin = frontendOrigin.replaceAll("/+$", "");
    }

    /**
     * 检查所需Delivery。
     * 主要协作调用：<code>from.isBlank</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     */
    @Override
    public void requireDelivery() {
        if (!enabled || from.isBlank()) {
            throw new ApiException("MAIL_NOT_CONFIGURED", "邮件服务尚未配置", 503);
        }
        outbox.requireReady();
    }

    /*
     * 邮件里的落地链接必须带账号树前缀 /useradmin。
     *
     * 入口改成「一个域名 + 两条路径树」之后，公开树里根本不存在重置密码与接受邀请的页面，
     * 只有账号树才有；链接不带前缀会被落成公开树首页，等于邮件里的链接直接失效。
     * 前缀在这里写死而不做成配置，是因为它与前端 shared/viewMode.js 的 ACCOUNT_PREFIX 是同一个契约，
     * 拆成两处配置只会让两边悄悄漂移。
     */
    /**
     * 重置地址。
     *
     * @param token 令牌；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String resetUrl(String token) {
        return frontendOrigin + "/useradmin/reset-password?token=" + token;
    }

    /**
     * 邀请地址处理入口。
     *
     * @param token 令牌；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String invitationUrl(String token) {
        return frontendOrigin + "/useradmin/invitation/accept?token=" + token;
    }

    /**
     * 发送Reset。
     * 关键处理步骤调用 <code>requireDelivery</code>、<code>send</code>、<code>resetUrl</code>。
     *
     * @param email 邮箱；类型为 <code>String</code>
     * @param token 令牌；类型为 <code>String</code>
     */
    @Override
    public void sendReset(String email, String token) {
        requireDelivery();
        send(email, "星雨笔录密码重置", "请在 30 分钟内打开以下链接重置密码：\n"
                + resetUrl(token) + "\n如果并非本人操作，请忽略此邮件。");
    }

    /**
     * 发送邮箱验证代码。
     * 关键处理步骤调用 <code>requireDelivery</code>、<code>send</code>。
     *
     * @param email 邮箱；类型为 <code>String</code>
     * @param code 代码；类型为 <code>String</code>
     */
    @Override
    public void sendEmailVerificationCode(String email, String code) {
        requireDelivery();
        send(email, "星雨笔录邮箱验证码", "你的邮箱验证码是：" + code
                + "，10 分钟内有效。\n请在星雨笔录页面输入验证码以完成邮箱验证。\n如果并非本人操作，请忽略此邮件。");
    }

    /**
     * 发送邀请。
     * 关键处理步骤调用 <code>requireDelivery</code>、<code>URI.create</code>、<code>Set.of</code>、<code>invitationUrl</code>。
     *
     * @param email 邮箱；类型为 <code>String</code>
     * @param token 令牌；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
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

    /**
     * 发送账号邮件。
     * 主要协作调用：<code>sender.createMimeMessage</code>、<code>sender.send</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param email 邮箱；类型为 <code>String</code>
     * @param subject subject；类型为 <code>String</code>
     * @param body 正文；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String send(String email, String subject, String body) {
        return outbox.enqueue(from, email, subject, body);
    }
}
