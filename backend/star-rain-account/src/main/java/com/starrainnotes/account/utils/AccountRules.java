package com.starrainnotes.account.utils;

import com.starrainnotes.common.exception.ApiException;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

public final class AccountRules {

    private AccountRules() {
    }

    public static long id(String value) {
        try {
            long id = Long.parseLong(value);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // The same public validation error applies to malformed and nonpositive ids.
        }
        throw new ApiException("INVALID_ID", "无效的账户或权限编号", 400);
    }

    public static String email(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static void password(String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new ApiException("PASSWORD_CONFIRM_MISMATCH", "两次输入的密码不一致", 400);
        }
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72 || password.isBlank()) {
            throw new ApiException("PASSWORD_POLICY_VIOLATION", "密码至少 12 个字符，UTF-8 编码不超过 72 字节", 400);
        }
    }
}

