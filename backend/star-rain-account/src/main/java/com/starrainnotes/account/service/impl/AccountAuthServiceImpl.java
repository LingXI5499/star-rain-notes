/*
 * AccountAuthServiceImpl：账号模块的业务实现文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：AccountEntity、PasswordResetEntity、RoleEntity、ChangePasswordDTO、ConfirmEmailDTO、LoginDTO、PasswordResetConfirmDTO。
 * 阅读入口：register（注册账号认证）；login（执行登录账号认证）；confirmEmail（确认邮箱）；logout（执行退出登录账号认证）。
 */

package com.starrainnotes.account.service.impl;

import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.entity.PasswordResetEntity;
import com.starrainnotes.account.entity.RoleEntity;
import com.starrainnotes.account.dto.ChangePasswordDTO;
import com.starrainnotes.account.dto.ConfirmEmailDTO;
import com.starrainnotes.account.dto.LoginDTO;
import com.starrainnotes.account.dto.PasswordResetConfirmDTO;
import com.starrainnotes.account.dto.PasswordResetRequestDTO;
import com.starrainnotes.account.dto.RegisterDTO;
import com.starrainnotes.account.dto.UpdateMyAccountDTO;
import com.starrainnotes.account.vo.CurrentAccountVO;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.account.context.AccountPrincipal;
import com.starrainnotes.account.service.AccountAuditService;
import com.starrainnotes.account.service.AccountThrottleService;
import com.starrainnotes.account.vo.LoginReservation;
import com.starrainnotes.account.service.AccountAuthService;
import com.starrainnotes.account.service.AccountIdentityService;
import com.starrainnotes.account.service.AccountMailService;
import com.starrainnotes.account.service.EmailVerificationService;
import com.starrainnotes.account.utils.AccountRules;
import com.starrainnotes.account.utils.OneTimeTokens;
import com.starrainnotes.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账号认证业务服务实现，属于账号模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class AccountAuthServiceImpl implements AccountAuthService {

    /**
     * 数据映射。
     * 协作类型：<code>AccountMapper</code>。
     */
    private final AccountMapper mapper;
    /**
     * 身份。
     * 协作类型：<code>AccountIdentityService</code>。
     */
    private final AccountIdentityService identity;
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
     * 认证Manager。
     */
    private final AuthenticationManager authenticationManager;
    /**
     * contextRepository。
     */
    private final SecurityContextRepository contextRepository;
    /**
     * 邮件。
     * 协作类型：<code>AccountMailService</code>。
     */
    private final AccountMailService mail;
    /**
     * 邮箱验证。
     * 协作类型：<code>EmailVerificationService</code>。
     */
    private final EmailVerificationService emailVerification;
    /** Independent, atomic source and identity reservations. */
    private final AccountThrottleService throttle;

    /**
     * 构造AccountAuthServiceImpl，接收本类使用的依赖或初始化参数。
     *
     * @param mapper 数据映射；类型为 <code>AccountMapper</code>
     * @param identity 身份；类型为 <code>AccountIdentityService</code>
     * @param audit 审计；类型为 <code>AccountAuditService</code>
     * @param encoder encoder；类型为 <code>PasswordEncoder</code>
     * @param authenticationManager 认证Manager；类型为 <code>AuthenticationManager</code>
     * @param contextRepository contextRepository；类型为 <code>SecurityContextRepository</code>
     * @param mail 邮件；类型为 <code>AccountMailService</code>
     * @param throttle 独立事务中的原子来源和身份限流
     * @param emailVerification 邮箱验证；类型为 <code>EmailVerificationService</code>
     */
    public AccountAuthServiceImpl(AccountMapper mapper, AccountIdentityService identity,
                                  AccountAuditService audit, PasswordEncoder encoder,
                                  AuthenticationManager authenticationManager,
                                  SecurityContextRepository contextRepository,
                                  AccountMailService mail, EmailVerificationService emailVerification,
                                  AccountThrottleService throttle) {
        this.mapper = mapper;
        this.identity = identity;
        this.audit = audit;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.mail = mail;
        this.emailVerification = emailVerification;
        this.throttle = throttle;
    }

    /**
     * 注册账号认证。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.usernameCount</code>、<code>mapper.emailCount</code>、<code>emailVerification.verify</code>、<code>emailVerification.consume</code>、<code>mapper.insertAccount</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>RegisterDTO</code>
     * @return 类型为 <code>CurrentAccountVO</code> 的处理结果
     */
    @Override
    @Transactional
    public CurrentAccountVO register(RegisterDTO request) {
        AccountRules.password(request.getPassword(), request.getConfirmPassword());
        String username = request.getUsername().trim();
        String email = AccountRules.email(request.getEmail());
        String codeHash = emailVerification.verify(email, request.getVerificationCode());
        if (mapper.usernameCount(username) > 0) {
            throw new ApiException("USERNAME_ALREADY_EXISTS", "用户名已被使用", 409);
        }
        if (mapper.emailCount(email) > 0) {
            throw new ApiException("EMAIL_ALREADY_EXISTS", "邮箱已被使用", 409);
        }
        RoleEntity userRole = requiredRole("USER");
        emailVerification.consume(email, codeHash);
        AccountEntity account = new AccountEntity();
        account.setUsername(username);
        account.setEmail(email);
        account.setEmailVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
        account.setDisplayName(username);
        account.setStatus("ACTIVE");
        try {
            mapper.insertAccount(account);
        } catch (DuplicateKeyException exception) {
            throw new ApiException("ACCOUNT_ALREADY_EXISTS", "用户名或邮箱已被使用", 409);
        }
        mapper.insertCredential(account.getId(), encoder.encode(request.getPassword()));
        mapper.insertAccountRole(account.getId(), userRole.getId(), null);
        audit.success(account.getId(), account.getId(), "REGISTER_SUCCESS");
        return identity.view(account.getId());
    }

    /**
     * 执行登录账号认证。
     * 主要协作调用：<code>throttle.reserveLogin</code>、<code>authenticationManager.authenticate</code>、<code>audit.failed</code>、<code>mapper.updateLastLogin</code>、<code>audit.success</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>LoginDTO</code>
     * @param servletRequest servlet请求；类型为 <code>HttpServletRequest</code>
     * @param servletResponse servlet响应；类型为 <code>HttpServletResponse</code>
     * @return 类型为 <code>CurrentAccountVO</code> 的处理结果
     */
    @Override
    public CurrentAccountVO login(LoginDTO request, HttpServletRequest servletRequest,
                             HttpServletResponse servletResponse) {
        LoginReservation reservation = throttle.reserveLogin(servletRequest.getRemoteAddr(), request.getIdentifier());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getIdentifier().trim(), request.getPassword()));
        } catch (AuthenticationException exception) {
            audit.failed(null, null, "LOGIN_FAILED");
            throw new ApiException("INVALID_CREDENTIALS", "用户名或密码错误", 401);
        }
        throttle.loginSucceeded(reservation);
        AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
        mapper.updateLastLogin(principal.getAccountId());
        audit.success(principal.getAccountId(), principal.getAccountId(), "LOGIN_SUCCESS");
        servletRequest.getSession(true);
        servletRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, servletRequest, servletResponse);
        return identity.view(principal.getAccountId());
    }

    /**
     * 确认邮箱。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>identity.principal</code>、<code>mapper.accountByIdForUpdate</code>、<code>emailVerification.verify</code>、<code>emailVerification.consume</code>、<code>mapper.verifyEmail</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ConfirmEmailDTO</code>
     * @return 类型为 <code>CurrentAccountVO</code> 的处理结果
     */
    @Override
    @Transactional
    public CurrentAccountVO confirmEmail(ConfirmEmailDTO request) {
        long id = identity.principal().getAccountId();
        AccountEntity account = mapper.accountByIdForUpdate(id);
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("ACCOUNT_DISABLED", "账户不可用", 403);
        }
        if (account.getEmailVerifiedAt() != null) {
            throw new ApiException("EMAIL_ALREADY_VERIFIED", "邮箱已经验证", 409);
        }
        String codeHash = emailVerification.verify(account.getEmail(), request.getVerificationCode());
        emailVerification.consume(account.getEmail(), codeHash);
        mapper.verifyEmail(id);
        audit.success(id, id, "EMAIL_VERIFIED");
        return identity.view(id);
    }

    /**
     * 执行退出登录账号认证。
     * 主要协作调用：<code>identity.currentOptional</code>、<code>audit.success</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>HttpServletRequest</code>
     */
    @Override
    public void logout(HttpServletRequest request) {
        identity.currentOptional().ifPresent(actor ->
                audit.success(actor.getAccountId(), actor.getAccountId(), "LOGOUT"));
        HttpSession session = request.getSession(false);
        SecurityContextHolder.clearContext();
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * 更新Me。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>identity.principal</code>、<code>mapper.updateDisplayName</code>、<code>identity.view</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>UpdateMyAccountDTO</code>
     * @return 类型为 <code>CurrentAccountVO</code> 的处理结果
     */
    @Override
    @Transactional
    public CurrentAccountVO updateMe(UpdateMyAccountDTO request) {
        long accountId = identity.principal().getAccountId();
        mapper.updateDisplayName(accountId, request.getDisplayName().trim());
        return identity.view(accountId);
    }

    /**
     * change密码处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>identity.principal</code>、<code>mapper.accountByIdForUpdate</code>、<code>mapper.passwordHash</code>、<code>encoder.matches</code>、<code>mapper.updatePassword</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ChangePasswordDTO</code>
     */
    @Override
    @Transactional
    public void changePassword(ChangePasswordDTO request) {
        AccountRules.password(request.getNewPassword(), request.getConfirmPassword());
        long accountId = identity.principal().getAccountId();
        AccountEntity account = mapper.accountByIdForUpdate(accountId);
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("ACCOUNT_DISABLED", "账户不可用", 403);
        }
        String hash = mapper.passwordHash(accountId);
        if (hash == null || !encoder.matches(request.getCurrentPassword(), hash)) {
            throw new ApiException("INVALID_CURRENT_PASSWORD", "当前密码不正确", 400);
        }
        if (encoder.matches(request.getNewPassword(), hash)) {
            throw new ApiException("PASSWORD_POLICY_VIOLATION", "新密码不能与当前密码相同", 400);
        }
        mapper.updatePassword(accountId, encoder.encode(request.getNewPassword()));
        mapper.incrementAuthVersion(accountId);
        audit.success(accountId, accountId, "PASSWORD_CHANGED");
    }

    /**
     * 请求密码Reset处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mail.requireDelivery</code>、<code>throttle.reserveReset</code>、<code>mail.requestReset</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>PasswordResetRequestDTO</code>
     * @param servletRequest servlet请求；类型为 <code>HttpServletRequest</code>
     */
    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequestDTO request, HttpServletRequest servletRequest) {
        mail.requireDelivery();
        String email = AccountRules.email(request.getEmail());
        throttle.reserveReset(servletRequest.getRemoteAddr(), email);
        // Resolve account existence only in the existing asynchronous encrypted outbox worker.
        // Both known and unknown addresses perform the same HTTP-side work.
        mail.requestReset(email);
        audit.success(null, null, "PASSWORD_RESET_REQUESTED");
    }

    /**
     * 确认密码Reset。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.resetByHashForUpdate</code>、<code>mapper.accountByIdForUpdate</code>、<code>mapper.useReset</code>、<code>mapper.updatePassword</code>、<code>encoder.encode</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>PasswordResetConfirmDTO</code>
     */
    @Override
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmDTO request) {
        AccountRules.password(request.getNewPassword(), request.getConfirmPassword());
        String tokenHash = OneTimeTokens.hash(request.getToken());
        PasswordResetEntity reference = mapper.resetByHash(tokenHash);
        if (reference == null) {
            throw new ApiException("INVALID_PASSWORD_RESET_TOKEN", "重置链接无效", 400);
        }
        // Reset delivery and confirmation both lock account before token to avoid inversion.
        AccountEntity account = mapper.accountByIdForUpdate(reference.getAccountId());
        PasswordResetEntity reset = mapper.resetByHashForUpdate(tokenHash);
        if (!"PENDING".equals(reset.getStatus())) {
            throw new ApiException("PASSWORD_RESET_TOKEN_USED", "重置链接已使用", 409);
        }
        if (!reset.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new ApiException("PASSWORD_RESET_TOKEN_EXPIRED", "重置链接已过期", 409);
        }
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("ACCOUNT_DISABLED", "账户不可用", 403);
        }
        if (mapper.useReset(reset.getId()) != 1) {
            throw new ApiException("PASSWORD_RESET_TOKEN_USED", "重置链接已使用", 409);
        }
        mapper.updatePassword(account.getId(), encoder.encode(request.getNewPassword()));
        mapper.expirePendingResets(account.getId());
        mapper.incrementAuthVersion(account.getId());
        audit.success(account.getId(), account.getId(), "PASSWORD_RESET_SUCCESS");
    }

    /**
     * 初始化Super后台处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.lockSuperAdminRole</code>、<code>mapper.activeSuperAdminCount</code>、<code>mapper.superAdminAssignmentCount</code>、<code>mapper.usernameCount</code>、<code>mapper.emailCount</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param enabled 已启用；类型为 <code>boolean</code>
     * @param username 用户名；类型为 <code>String</code>
     * @param email 邮箱；类型为 <code>String</code>
     * @param password 密码参数；使用和保存方式由对应的密码处理流程决定；类型为 <code>String</code>
     */
    @Override
    @Transactional
    public void bootstrapSuperAdmin(boolean enabled, String username, String email, String password) {
        mapper.lockSuperAdminRole();
        if (mapper.activeSuperAdminCount() > 0 || !enabled) {
            return;
        }
        if (mapper.superAdminAssignmentCount() > 0) {
            throw new IllegalStateException("No active Super Admin; recover an existing administrator");
        }
        if (username == null || !username.matches("[A-Za-z0-9_]{3,50}")
                || email == null || email.isBlank() || password == null) {
            throw new IllegalStateException("Bootstrap administrator settings are incomplete");
        }
        AccountRules.password(password, password);
        String normalizedEmail = AccountRules.email(email);
        if (mapper.usernameCount(username) > 0 || mapper.emailCount(normalizedEmail) > 0) {
            throw new IllegalStateException("Bootstrap administrator conflicts with an existing account");
        }
        AccountEntity account = new AccountEntity();
        account.setUsername(username);
        account.setEmail(normalizedEmail);
        account.setDisplayName(username);
        account.setStatus("ACTIVE");
        mapper.insertAccount(account);
        mapper.insertCredential(account.getId(), encoder.encode(password));
        for (String code : List.of("USER", "ADMIN", "SUPER_ADMIN")) {
            mapper.insertAccountRole(account.getId(), requiredRole(code).getId(), null);
        }
        audit.success(account.getId(), account.getId(), "BOOTSTRAP_SUPER_ADMIN_CREATED");
    }

    /**
     * 必需角色处理入口。
     * 主要协作调用：<code>mapper.roleByCode</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param code 代码；类型为 <code>String</code>
     * @return 类型为 <code>RoleEntity</code> 的处理结果
     */
    private RoleEntity requiredRole(String code) {
        RoleEntity role = mapper.roleByCode(code);
        if (role == null || !"ENABLED".equals(role.getStatus())) {
            throw new IllegalStateException("Missing active built-in role: " + code);
        }
        return role;
    }
}
