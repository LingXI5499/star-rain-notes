package com.starrainnotes.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.account.entity.AccountUser;
import com.starrainnotes.account.mapper.AccountUserMapper;
import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AccountService {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";
    public static final String PENDING = "PENDING_ACTIVATION";
    public static final String ACTIVE = "ACTIVE";
    public static final String DISABLED = "DISABLED";

    private final AccountUserMapper userMapper;

    public AccountService(AccountUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public List<String> capabilities(String role) {
        if (SUPER_ADMIN.equals(role)) {
            return List.of("TUTORIAL_COLLABORATE", "BLOG_COLLABORATE", "ENGLISH_COLLABORATE",
                    "SUPER_ADMIN", "REVIEW");
        }
        return List.of("TUTORIAL_COLLABORATE", "BLOG_COLLABORATE", "ENGLISH_COLLABORATE");
    }

    private void requireSuperAdmin(Long accountId, String detail) {
        AccountUser user = requireById(accountId);
        if (!SUPER_ADMIN.equals(user.getRole())) {
            throw fail("FORBIDDEN_MODULE", HttpStatus.FORBIDDEN, "Forbidden", detail);
        }
    }

    private AccountUser requireById(Long id) {
        AccountUser user = userMapper.selectById(id);
        if (user == null) throw fail("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found", "No such account.");
        return user;
    }
    private AccountUser findByEmail(String email) {
        return userMapper.selectOne(new LambdaQueryWrapper<AccountUser>().eq(AccountUser::getEmail, email)
                .last("LIMIT 1"));
    }
    public static String normalize(String email) { return email == null ? null : email.trim().toLowerCase(); }
    public static boolean validEmail(String email) {
        return email != null && email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
    public static String mask(String email) {
        if (email == null || email.isBlank()) return "";
        int at = email.indexOf('@');
        return at <= 0 ? "***" : email.substring(0, 2) + "***" + email.substring(at);
    }
    public static ApiException fail(String code, HttpStatus status, String title, String detail) {
        return new ApiException(status, code, title, detail);
    }
    public void requireSuperAdminOr401(Long actorId) {
        if (actorId == null) {
            throw fail("UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "Not authenticated",
                    "Super-admin authentication is required.");
        }
        requireSuperAdmin(actorId, "Super-admin authentication is required.");
    }
    public AccountUser findByEmailPublic(String email) {
        return findByEmail(normalize(email));
    }
}
