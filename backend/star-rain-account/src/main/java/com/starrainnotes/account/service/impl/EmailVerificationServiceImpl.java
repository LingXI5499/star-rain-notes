/*
 * EmailVerificationServiceImpl：账号模块的业务实现文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：EmailVerificationEntity、AccountEntity、AccountMapper、EmailVerificationMapper、AccountAuditService、AccountIdentityService、AccountMailService。
 * 阅读入口：send（发送邮箱验证）；sendForCurrentAccount（发送面向当前账号）；verify（验证邮箱验证）；consume（消费邮箱验证）。
 */

package com.starrainnotes.account.service.impl;

import com.starrainnotes.account.entity.EmailVerificationEntity;
import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.account.mapper.EmailVerificationMapper;
import com.starrainnotes.account.service.AccountAuditService;
import com.starrainnotes.account.service.AccountIdentityService;
import com.starrainnotes.account.service.AccountMailService;
import com.starrainnotes.account.service.EmailVerificationService;
import com.starrainnotes.account.utils.AccountRules;
import com.starrainnotes.account.vo.EmailVerificationCodeVO;
import com.starrainnotes.common.exception.ApiException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 邮箱验证业务服务实现，属于账号模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {
    /**
     * RANDOM。
     */
    private static final SecureRandom RANDOM = new SecureRandom();
    /**
     * 秒。
     * 初始值为 <code>600</code>。
     */
    private static final int VALID_SECONDS = 600;
    /**
     * 秒。
     * 初始值为 <code>60</code>。
     */
    private static final int RESEND_SECONDS = 60;
    /**
     * FAILURES。
     * 初始值为 <code>5</code>。
     */
    private static final int MAX_FAILURES = 5;

    /**
     * 数据映射。
     * 协作类型：<code>EmailVerificationMapper</code>。
     */
    private final EmailVerificationMapper mapper;
    /**
     * 账号集合。
     * 协作类型：<code>AccountMapper</code>。
     */
    private final AccountMapper accounts;
    /**
     * 邮件。
     * 协作类型：<code>AccountMailService</code>。
     */
    private final AccountMailService mail;
    /**
     * 审计。
     * 协作类型：<code>AccountAuditService</code>。
     */
    private final AccountAuditService audit;
    /**
     * encoder。
     */
    private final PasswordEncoder encoder;
    /**
     * 身份。
     * 协作类型：<code>AccountIdentityService</code>。
     */
    private final AccountIdentityService identity;

    /**
     * 构造EmailVerificationServiceImpl，接收本类使用的依赖或初始化参数。
     *
     * @param mapper 数据映射；类型为 <code>EmailVerificationMapper</code>
     * @param accounts 账号集合；类型为 <code>AccountMapper</code>
     * @param mail 邮件；类型为 <code>AccountMailService</code>
     * @param audit 审计；类型为 <code>AccountAuditService</code>
     * @param encoder encoder；类型为 <code>PasswordEncoder</code>
     * @param identity 身份；类型为 <code>AccountIdentityService</code>
     */
    public EmailVerificationServiceImpl(EmailVerificationMapper mapper, AccountMapper accounts,
                                        AccountMailService mail, AccountAuditService audit,
                                        PasswordEncoder encoder, AccountIdentityService identity) {
        this.mapper = mapper;
        this.accounts = accounts;
        this.mail = mail;
        this.audit = audit;
        this.encoder = encoder;
        this.identity = identity;
    }

    /**
     * 发送邮箱验证。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mail.requireDelivery</code>、<code>accounts.accountByEmail</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param rawEmail 原始邮箱；类型为 <code>String</code>
     * @param ip IP 地址；类型为 <code>String</code>
     * @return 类型为 <code>EmailVerificationCodeVO</code> 的处理结果
     */
    @Override
    @Transactional
    public EmailVerificationCodeVO send(String rawEmail, String ip) {
        mail.requireDelivery();
        String email = AccountRules.email(rawEmail);
        AccountEntity existing = accounts.accountByEmail(email);
        if (existing != null && (existing.getEmailVerifiedAt() != null || !"ACTIVE".equals(existing.getStatus()))) {
            throw new ApiException("EMAIL_ALREADY_EXISTS", "该邮箱已注册，请直接登录", 409);
        }
        // 注册流程没有登录主体，失败审计的 actor 为空
        return sendCode(email, ip, existing != null, null);
    }

    /**
     * 发送面向当前账号。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>accounts.accountByIdForUpdate</code>、<code>identity.principal</code>、<code>mail.requireDelivery</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param ip IP 地址；类型为 <code>String</code>
     * @return 类型为 <code>EmailVerificationCodeVO</code> 的处理结果
     */
    @Override
    @Transactional
    public EmailVerificationCodeVO sendForCurrentAccount(String ip) {
        AccountEntity account = accounts.accountByIdForUpdate(identity.principal().getAccountId());
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("ACCOUNT_DISABLED", "账户不可用", 403);
        }
        if (account.getEmailVerifiedAt() != null) {
            throw new ApiException("EMAIL_ALREADY_VERIFIED", "邮箱已经验证", 409);
        }
        mail.requireDelivery();
        return sendCode(account.getEmail(), ip, true, account.getId());
    }

    /**
     * 发送代码。
     * 主要协作调用：<code>accounts.recentAuditCount</code>、<code>mapper.ensureEmail</code>、<code>mapper.findForUpdate</code>、<code>RANDOM.nextInt</code>、<code>encoder.encode</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param email 邮箱；类型为 <code>String</code>
     * @param ip IP 地址；类型为 <code>String</code>
     * @param existingAccount existing账号；类型为 <code>boolean</code>
     * @param actorId 操作者标识；类型为 <code>Long</code>
     * @return 类型为 <code>EmailVerificationCodeVO</code> 的处理结果
     */
    private EmailVerificationCodeVO sendCode(String email, String ip, boolean existingAccount, Long actorId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int requests = accounts.recentAuditCount(ip, "EMAIL_CODE_SENT", now.minusHours(1))
                + accounts.recentAuditCount(ip, "EMAIL_CODE_DELIVERY_FAILED", now.minusHours(1));
        if (requests >= 10) {
            throw new ApiException("EMAIL_CODE_RATE_LIMITED", "验证码请求过于频繁，请一小时后再试", 429);
        }
        // 同一邮箱的发送请求通过行锁串行处理，冷却和次数限制由服务器执行。
        mapper.ensureEmail(email);
        EmailVerificationEntity verification = mapper.findForUpdate(email);
        if (verification.getSentAt() != null
                && verification.getSentAt().plusSeconds(RESEND_SECONDS).isAfter(now)) {
            throw new ApiException("EMAIL_CODE_COOLDOWN", "请等待 60 秒后再发送验证码", 429);
        }
        if (verification.getWindowStartedAt() == null
                || !verification.getWindowStartedAt().plusHours(1).isAfter(now)) {
            verification.setWindowStartedAt(now);
            verification.setSendCount(0);
        }
        if (verification.getSendCount() >= 5) {
            throw new ApiException("EMAIL_CODE_RATE_LIMITED", "该邮箱验证码请求过于频繁，请一小时后再试", 429);
        }
        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        verification.setCodeHash(encoder.encode(code));
        verification.setExpiresAt(now.plusSeconds(VALID_SECONDS));
        verification.setSentAt(now);
        verification.setSendCount(verification.getSendCount() + 1);
        deliver(email, code, actorId);
        mapper.saveCode(verification);
        audit.success(null, null, "EMAIL_CODE_SENT");
        return new EmailVerificationCodeVO(VALID_SECONDS, RESEND_SECONDS, existingAccount);
    }

    /*
     * 发信失败要留一条审计，而这条审计描述的是「本次事务失败」这件事本身，
     * 所以必须写到独立事务里（failedIndependently），否则会跟着本方法的回滚一起消失。
     *
     * 这条规则原先写在两个 Controller 的 catch 里 —— 在那里它恰好能活下来，
     * 因为事务已经回滚完、写审计落到了自动提交上。但那条调用路径把业务规则留在了接入层，
     * 且「能活下来」依赖的是调用时机而不是声明的语义，一旦有人把它搬进来就会静默丢审计。
     */
    /**
     * deliver处理入口。
     * 主要协作调用：<code>mail.sendEmailVerificationCode</code>、<code>audit.failedIndependently</code>。
     *
     * @param email 邮箱；类型为 <code>String</code>
     * @param code 代码；类型为 <code>String</code>
     * @param actorId 操作者标识；类型为 <code>Long</code>
     */
    private void deliver(String email, String code, Long actorId) {
        try {
            mail.sendEmailVerificationCode(email, code);
        } catch (ApiException exception) {
            if ("MAIL_DELIVERY_FAILED".equals(exception.getCode())) {
                audit.failedIndependently(actorId, null, "EMAIL_CODE_DELIVERY_FAILED");
            }
            throw exception;
        }
    }

    // 失败计数独立提交，避免注册事务回滚后验证码可以被无限试错。
    // 成功时只返回哈希；验证码的消费仍参与注册事务，注册失败不会烧毁验证码。
    /**
     * 验证邮箱验证。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.findForUpdate</code>、<code>encoder.matches</code>、<code>mapper.incrementFailures</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param email 邮箱；类型为 <code>String</code>
     * @param code 代码；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)
    public String verify(String email, String code) {
        EmailVerificationEntity verification = mapper.findForUpdate(email);
        if (verification == null || verification.getCodeHash() == null) {
            throw new ApiException("EMAIL_CODE_REQUIRED", "请先获取邮箱验证码", 400);
        }
        if (verification.getConsumedAt() != null) {
            throw new ApiException("EMAIL_CODE_USED", "验证码已使用，请重新获取", 409);
        }
        if (!verification.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new ApiException("EMAIL_CODE_EXPIRED", "验证码已过期，请重新获取", 400);
        }
        if (verification.getFailedAttempts() >= MAX_FAILURES) {
            throw new ApiException("EMAIL_CODE_LOCKED", "验证码错误次数过多，请重新获取", 429);
        }
        if (!encoder.matches(code, verification.getCodeHash())) {
            mapper.incrementFailures(email);
            throw new ApiException("EMAIL_CODE_INVALID", "邮箱验证码不正确", 400);
        }
        return verification.getCodeHash();
    }

    /**
     * 消费邮箱验证。
     * 事务声明：<code>@Transactional(propagation = Propagation.MANDATORY)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.consume</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param email 邮箱；类型为 <code>String</code>
     * @param codeHash 代码摘要；类型为 <code>String</code>
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void consume(String email, String codeHash) {
        if (mapper.consume(email, codeHash) != 1) {
            throw new ApiException("EMAIL_CODE_INVALID", "验证码已失效，请重新获取", 409);
        }
    }
}
