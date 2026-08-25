package com.starrainnotes.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.auth.dto.AuthSessionView;
import com.starrainnotes.auth.entity.AdminUser;
import com.starrainnotes.auth.mapper.AdminUserMapper;
import com.starrainnotes.common.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Session authentication helpers (04-api-design.md §7): current session view,
 * last-login bookkeeping, logout and password change (which invalidates the
 * current session and forces re-login).
 */
@Service
public class AuthService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AdminUserMapper adminUserMapper, PasswordEncoder passwordEncoder) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthSessionView currentSessionView() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return AuthSessionView.anonymous();
        }
        if (authentication.getPrincipal() instanceof AccountPrincipal principal) {
            return AuthSessionView.authenticated(principal.getEmail(), "ROLE_" + principal.getRole());
        }
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);
        return AuthSessionView.authenticated(authentication.getName(), role);
    }

    public void recordLastLogin(String username) {
        AdminUser admin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username));
        if (admin != null) {
            admin.setLastLoginAt(LocalDateTime.now(Clock.systemUTC()));
            adminUserMapper.updateById(admin);
        }
    }

    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public void changePassword(String currentPassword, String newPassword, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AdminUser admin = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, authentication.getName()));
        if (admin == null || !passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_CURRENT_PASSWORD",
                    "Current password is incorrect",
                    "The current password does not match.");
        }
        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        adminUserMapper.updateById(admin);
        logout(request);
    }
}
