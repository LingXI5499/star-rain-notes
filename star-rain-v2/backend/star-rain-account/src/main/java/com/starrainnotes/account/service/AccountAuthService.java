package com.starrainnotes.account.service;

import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.support.AccountRules;
import com.starrainnotes.account.support.OneTimeTokens;
import com.starrainnotes.account.entity.RoleEntity;
import com.starrainnotes.account.entity.PasswordResetEntity;
import com.starrainnotes.account.dto.RegisterDTO;
import com.starrainnotes.account.dto.ConfirmEmailDTO;
import com.starrainnotes.account.dto.LoginDTO;
import com.starrainnotes.account.dto.UpdateMyAccountDTO;
import com.starrainnotes.account.dto.ChangePasswordDTO;
import com.starrainnotes.account.dto.PasswordResetRequestDTO;
import com.starrainnotes.account.dto.PasswordResetConfirmDTO;
import com.starrainnotes.account.vo.CurrentAccountVO;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
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

@Service
public class AccountAuthService {

    private final AccountMapper mapper;
    private final AccountIdentityService identity;
    private final AccountAuditService audit;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final AccountMailService mail;
    private final EmailVerificationService emailVerification;

    public AccountAuthService(AccountMapper mapper, AccountIdentityService identity,
                              AccountAuditService audit, PasswordEncoder encoder,
                              AuthenticationManager authenticationManager,
                              SecurityContextRepository contextRepository,
                              AccountMailService mail, EmailVerificationService emailVerification) {
        this.mapper = mapper;
        this.identity = identity;
        this.audit = audit;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.mail = mail;
        this.emailVerification = emailVerification;
    }

    @Transactional
    public CurrentAccountVO register(RegisterDTO request) {
        AccountRules.password(request.password(), request.confirmPassword());
        String username = request.username().trim();
        String email = AccountRules.email(request.email());
        if (mapper.usernameCount(username) > 0) {
            throw new ApiException("USERNAME_ALREADY_EXISTS", "用户名已被使用", 409);
        }
        if (mapper.emailCount(email) > 0) {
            throw new ApiException("EMAIL_ALREADY_EXISTS", "邮箱已被使用", 409);
        }
        RoleEntity userRole = requiredRole("USER");
        String codeHash = emailVerification.verify(email, request.verificationCode());
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
        mapper.insertCredential(account.getId(), encoder.encode(request.password()));
        mapper.insertAccountRole(account.getId(), userRole.getId(), null);
        audit.success(account.getId(), account.getId(), "REGISTER_SUCCESS");
        return identity.view(account.getId());
    }

    public CurrentAccountVO login(LoginDTO request, HttpServletRequest servletRequest,
                             HttpServletResponse servletResponse) {
        String ip = servletRequest.getRemoteAddr();
        if (mapper.recentAuditCount(ip, "LOGIN_FAILED",
                LocalDateTime.now(ZoneOffset.UTC).minusMinutes(15)) >= 10) {
            throw new ApiException("LOGIN_RATE_LIMITED", "登录尝试过于频繁，请稍后再试", 429);
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.identifier().trim(), request.password()));
        } catch (AuthenticationException exception) {
            audit.failed(null, null, "LOGIN_FAILED");
            throw new ApiException("INVALID_CREDENTIALS", "身份信息无效", 401);
        }
        AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
        mapper.updateLastLogin(principal.accountId());
        audit.success(principal.accountId(), principal.accountId(), "LOGIN_SUCCESS");
        servletRequest.getSession(true);
        servletRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, servletRequest, servletResponse);
        return identity.view(principal.accountId());
    }

    @Transactional
    public CurrentAccountVO confirmEmail(ConfirmEmailDTO request) {
        long id = identity.principal().accountId();
        AccountEntity account = mapper.accountByIdForUpdate(id);
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("ACCOUNT_DISABLED", "账户不可用", 403);
        }
        if (account.getEmailVerifiedAt() != null) {
            throw new ApiException("EMAIL_ALREADY_VERIFIED", "邮箱已经验证", 409);
        }
        String codeHash = emailVerification.verify(account.getEmail(), request.verificationCode());
        emailVerification.consume(account.getEmail(), codeHash);
        mapper.verifyEmail(id);
        audit.success(id, id, "EMAIL_VERIFIED");
        return identity.view(id);
    }

    public void logout(HttpServletRequest request) {
        identity.currentOptional().ifPresent(actor ->
                audit.success(actor.accountId(), actor.accountId(), "LOGOUT"));
        HttpSession session = request.getSession(false);
        SecurityContextHolder.clearContext();
        if (session != null) {
            session.invalidate();
        }
    }

    @Transactional
    public CurrentAccountVO updateMe(UpdateMyAccountDTO request) {
        long accountId = identity.principal().accountId();
        mapper.updateDisplayName(accountId, request.displayName().trim());
        return identity.view(accountId);
    }

    @Transactional
    public void changePassword(ChangePasswordDTO request) {
        AccountRules.password(request.newPassword(), request.confirmPassword());
        long accountId = identity.principal().accountId();
        AccountEntity account = mapper.accountByIdForUpdate(accountId);
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("ACCOUNT_DISABLED", "账户不可用", 403);
        }
        String hash = mapper.passwordHash(accountId);
        if (hash == null || !encoder.matches(request.currentPassword(), hash)) {
            throw new ApiException("INVALID_CURRENT_PASSWORD", "当前密码不正确", 400);
        }
        if (encoder.matches(request.newPassword(), hash)) {
            throw new ApiException("PASSWORD_POLICY_VIOLATION", "新密码不能与当前密码相同", 400);
        }
        mapper.updatePassword(accountId, encoder.encode(request.newPassword()));
        mapper.incrementAuthVersion(accountId);
        audit.success(accountId, accountId, "PASSWORD_CHANGED");
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequestDTO request, HttpServletRequest servletRequest) {
        mail.requireDelivery();
        String ip = servletRequest.getRemoteAddr();
        if (mapper.recentResetRequestsByIp(ip,
                LocalDateTime.now(ZoneOffset.UTC).minusHours(1)) >= 5) {
            throw new ApiException("PASSWORD_RESET_RATE_LIMITED", "请求过于频繁，请稍后再试", 429);
        }
        AccountEntity account = mapper.accountByEmail(AccountRules.email(request.email()));
        if (account != null && "ACTIVE".equals(account.getStatus())) {
            mapper.expirePendingResets(account.getId());
            String token = OneTimeTokens.create();
            PasswordResetEntity reset = new PasswordResetEntity();
            reset.setAccountId(account.getId());
            reset.setTokenHash(OneTimeTokens.hash(token));
            reset.setStatus("PENDING");
            reset.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30));
            mapper.insertReset(reset);
            mail.sendReset(account.getEmail(), token);
        }
        audit.success(null, account == null ? null : account.getId(), "PASSWORD_RESET_REQUESTED");
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmDTO request) {
        AccountRules.password(request.newPassword(), request.confirmPassword());
        PasswordResetEntity reset = mapper.resetByHashForUpdate(OneTimeTokens.hash(request.token()));
        if (reset == null) {
            throw new ApiException("INVALID_PASSWORD_RESET_TOKEN", "重置链接无效", 400);
        }
        if (!"PENDING".equals(reset.getStatus())) {
            throw new ApiException("PASSWORD_RESET_TOKEN_USED", "重置链接已使用", 409);
        }
        if (!reset.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new ApiException("PASSWORD_RESET_TOKEN_EXPIRED", "重置链接已过期", 409);
        }
        AccountEntity account = mapper.accountByIdForUpdate(reset.getAccountId());
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("ACCOUNT_DISABLED", "账户不可用", 403);
        }
        if (mapper.useReset(reset.getId()) != 1) {
            throw new ApiException("PASSWORD_RESET_TOKEN_USED", "重置链接已使用", 409);
        }
        mapper.updatePassword(account.getId(), encoder.encode(request.newPassword()));
        mapper.expirePendingResets(account.getId());
        mapper.incrementAuthVersion(account.getId());
        audit.success(account.getId(), account.getId(), "PASSWORD_RESET_SUCCESS");
    }

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

    private RoleEntity requiredRole(String code) {
        RoleEntity role = mapper.roleByCode(code);
        if (role == null || !"ENABLED".equals(role.getStatus())) {
            throw new IllegalStateException("Missing active built-in role: " + code);
        }
        return role;
    }
}

