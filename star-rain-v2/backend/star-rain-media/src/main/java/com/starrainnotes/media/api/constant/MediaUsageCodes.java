package com.starrainnotes.media.api.constant;

import java.util.Locale;
import java.util.Set;

/*
 * 各业务模块登记的稳定 usageCode。
 *
 * Media 不建立全局巨型 Enum，也不理解业务语义；这里只登记既有约定，
 * 便于跨模块一致性与排障。业务模块新增用途时先在此登记再使用。
 *
 * 约束：usageCode 形如 <module>.<purpose>，且前缀等于 sourceModule 的小写形式。
 */
public final class MediaUsageCodes {

    public static final String BLOG_COVER = "blog.cover";
    public static final String BLOG_CONTENT = "blog.content";

    public static final String TUTORIAL_COVER = "tutorial.cover";
    public static final String TUTORIAL_CHAPTER_CONTENT = "tutorial.chapter-content";

    public static final String PORTFOLIO_COVER = "portfolio.cover";
    public static final String PORTFOLIO_SCREENSHOT = "portfolio.screenshot";
    public static final String PORTFOLIO_AUDIO = "portfolio.audio";
    public static final String PORTFOLIO_ATTACHMENT = "portfolio.attachment";
    public static final String PORTFOLIO_SECTION = "portfolio.section";
    public static final String PORTFOLIO_PROTOTYPE = "portfolio.prototype";

    public static final String PROFILE_AVATAR = "profile.avatar";
    public static final String PROFILE_RESUME = "profile.resume";

    public static final String SITE_LOGO = "site.logo";
    public static final String SITE_FAVICON = "site.favicon";

    private static final Set<Character> EXTRA_PREFIX_CHARS = Set.of('-', '_');

    private MediaUsageCodes() {
    }

    // usageCode 前缀必须与 sourceModule 一致，避免把 blog.cover 挂到 TUTORIAL 上
    public static boolean matchesSourceModule(String usageCode, String sourceModule) {
        if (usageCode == null || sourceModule == null) {
            return false;
        }
        String expectedPrefix = sourceModule.trim().toLowerCase(Locale.ROOT);
        return usageCode.trim().toLowerCase(Locale.ROOT).startsWith(expectedPrefix + ".");
    }

    // 形如 blog.cover：一段小写前缀 + 一段用途，用途段允许再带点号与连字符
    public static boolean hasValidFormat(String usageCode) {
        if (usageCode == null) {
            return false;
        }
        String value = usageCode.trim();
        int dot = value.indexOf('.');
        if (dot <= 0 || dot == value.length() - 1) {
            return false;
        }
        String prefix = value.substring(0, dot);
        String purpose = value.substring(dot + 1);
        return isSegment(prefix, false) && isSegment(purpose, true);
    }

    private static boolean isSegment(String segment, boolean allowDots) {
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || EXTRA_PREFIX_CHARS.contains(c)
                    || (allowDots && c == '.');
            if (!ok) {
                return false;
            }
        }
        return !segment.isEmpty();
    }
}
