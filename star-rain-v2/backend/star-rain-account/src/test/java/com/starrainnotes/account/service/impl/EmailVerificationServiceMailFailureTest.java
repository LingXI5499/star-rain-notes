package com.starrainnotes.account.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.account.context.AccountPrincipal;
import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.entity.EmailVerificationEntity;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.account.mapper.EmailVerificationMapper;
import com.starrainnotes.account.service.AccountAuditService;
import com.starrainnotes.account.service.AccountIdentityService;
import com.starrainnotes.account.service.AccountMailService;
import com.starrainnotes.common.exception.ApiException;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/*
 * 「邮件投递失败要留审计」这条规则的回归测试。
 *
 * 背景：这条规则原先写在 AccountController / AuthController 的 catch 里。在那里它确实能生效，
 * 但生效的原因是**调用时机**（服务事务已回滚，写审计落到自动提交上），而不是声明的语义 ——
 * 谁把它搬进 @Transactional 的服务方法、又用普通的 audit.failed()，这条审计就会静默消失。
 *
 * 现在的实现把它收回服务层，并改用 AccountAuditService.failedIndependently（REQUIRES_NEW）：
 * 语义写进了方法名，不再依赖调用点。这个测试钉住三件事：
 *   1. 发信失败时确实调用了独立事务版本的审计（而不是会跟着回滚的 failed）；
 *   2. 异常继续向上抛，接口仍然返回失败，不会假装成功；
 *   3. 验证码没有被写库（发信失败就不该留下一个能用的验证码）。
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceMailFailureTest {

    private static final long ACCOUNT_ID = 4242L;
    private static final String EMAIL = "owner@example.test";

    @Mock private EmailVerificationMapper mapper;
    @Mock private AccountMapper accounts;
    @Mock private AccountMailService mail;
    @Mock private AccountAuditService audit;
    @Mock private PasswordEncoder encoder;
    @Mock private AccountIdentityService identity;

    private EmailVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationServiceImpl(mapper, accounts, mail, audit, encoder, identity);
    }

    @Test
    void deliveryFailureIsAuditedInItsOwnTransactionAndStillFailsTheRequest() {
        givenSendableAccount();
        // 返回 void 的方法不能用 when(...)，必须用 doThrow(...).when(...)
        doThrow(new ApiException("MAIL_DELIVERY_FAILED", "邮件发送失败，请稍后重试", 503))
                .when(mail).sendEmailVerificationCode(eq(EMAIL), anyString());

        ApiException failure = assertThrows(ApiException.class, () -> service.sendForCurrentAccount("127.0.0.1"));

        assertEquals("MAIL_DELIVERY_FAILED", failure.getCode());
        verify(audit).failedIndependently(ACCOUNT_ID, null, "EMAIL_CODE_DELIVERY_FAILED");
        // 用普通 failed() 会让这条审计跟着事务回滚一起消失，因此绝不允许退化成它
        verify(audit, never()).failed(any(), any(), anyString());
        verify(audit, never()).success(any(), any(), anyString());
        verify(mapper, never()).saveCode(any(EmailVerificationEntity.class));
    }

    @Test
    void registrationPathAuditsFailureWithoutAnActor() {
        when(accounts.accountByEmail(EMAIL)).thenReturn(null);
        when(accounts.recentAuditCount(anyString(), anyString(), any(LocalDateTime.class))).thenReturn(0);
        when(mapper.findForUpdate(EMAIL)).thenReturn(freshVerification());
        when(encoder.encode(anyString())).thenReturn("hash");
        doThrow(new ApiException("MAIL_DELIVERY_FAILED", "邮件发送失败，请稍后重试", 503))
                .when(mail).sendEmailVerificationCode(eq(EMAIL), anyString());

        assertThrows(ApiException.class, () -> service.send(EMAIL, "127.0.0.1"));

        // 注册流程没有登录主体，actor 必须是 null（与改动前 Controller 里的行为一致）
        verify(audit).failedIndependently(null, null, "EMAIL_CODE_DELIVERY_FAILED");
    }

    @Test
    void successfulDeliveryAuditsSuccessAndNeverTheFailureRecord() {
        givenSendableAccount();

        service.sendForCurrentAccount("127.0.0.1");

        verify(audit).success(null, null, "EMAIL_CODE_SENT");
        verify(audit, never()).failedIndependently(any(), any(), anyString());
    }

    private void givenSendableAccount() {
        AccountEntity account = new AccountEntity();
        account.setId(ACCOUNT_ID);
        account.setEmail(EMAIL);
        account.setStatus("ACTIVE");
        account.setEmailVerifiedAt(null);
        // 注意：不能用 AccountPrincipal.builder() 只设 accountId ——
        // 它的全参构造器会执行 Set.copyOf(roles)，roles 为 null 时直接 NPE。
        when(identity.principal()).thenReturn(new AccountPrincipal(ACCOUNT_ID, "owner", 1, Set.of(), Set.of()));
        when(accounts.accountByIdForUpdate(ACCOUNT_ID)).thenReturn(account);
        when(accounts.recentAuditCount(anyString(), anyString(), any(LocalDateTime.class))).thenReturn(0);
        when(mapper.findForUpdate(EMAIL)).thenReturn(freshVerification());
        when(encoder.encode(anyString())).thenReturn("hash");
    }

    private EmailVerificationEntity freshVerification() {
        EmailVerificationEntity entity = new EmailVerificationEntity();
        entity.setEmail(EMAIL);
        entity.setSendCount(0);
        return entity;
    }
}
