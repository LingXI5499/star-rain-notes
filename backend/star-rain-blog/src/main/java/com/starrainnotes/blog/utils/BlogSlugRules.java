package com.starrainnotes.blog.utils;

import java.util.Locale;

/*
 * slug 规范化与格式校验。
 *
 * 规则：小写字母 / 数字，段之间用单个中划线连接。
 * 为什么统一转小写：slug 会出现在 URL 里，/Blog/Hello 与 /blog/hello 在部分服务器上
 * 会被当成两个不同页面，造成重复内容。这里在入口处就收敛成唯一写法，
 * 数据库唯一键因此也只需要比较一种形态。
 */
public final class BlogSlugRules {

    private static final String SLUG_PATTERN = "[a-z0-9]+(?:-[a-z0-9]+)*";

    private BlogSlugRules() {
    }

    // 去掉首尾空白并转小写；null / 空白返回 null，由调用方决定默认值
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    // 规范化之后再判断，保证 "Hello-World" 这种输入是被接受而不是被拒绝
    public static boolean isValid(String normalized, int maxLength) {
        if (normalized == null || normalized.length() > maxLength) {
            return false;
        }
        return normalized.matches(SLUG_PATTERN);
    }
}
