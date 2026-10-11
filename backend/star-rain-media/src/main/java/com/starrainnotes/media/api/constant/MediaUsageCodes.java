/*
 * MediaUsageCodes：媒体模块的跨模块接口文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：matchesSourceModule（判断是否匹配来源Module）；hasValidFormat（判断是否具备有效格式）。
 */

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
/**
 * 媒体UsageCodes，属于媒体模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public final class MediaUsageCodes {

    /**
     * 封面。
     */
    public static final String BLOG_COVER = "blog.cover";
    /**
     * 内容。
     */
    public static final String BLOG_CONTENT = "blog.content";

    /**
     * 封面。
     */
    public static final String TUTORIAL_COVER = "tutorial.cover";
    /**
     * 内容。
     */
    public static final String TUTORIAL_CHAPTER_CONTENT = "tutorial.chapter-content";

    /**
     * 封面。
     */
    public static final String PORTFOLIO_COVER = "portfolio.cover";
    /**
     * 截图。
     */
    public static final String PORTFOLIO_SCREENSHOT = "portfolio.screenshot";
    /**
     * 音频。
     */
    public static final String PORTFOLIO_AUDIO = "portfolio.audio";
    /**
     * ATTACHMENT。
     */
    public static final String PORTFOLIO_ATTACHMENT = "portfolio.attachment";
    /**
     * 区块。
     */
    public static final String PORTFOLIO_SECTION = "portfolio.section";
    /**
     * PROTOTYPE。
     */
    public static final String PORTFOLIO_PROTOTYPE = "portfolio.prototype";

    /**
     * AVATAR。
     */
    public static final String PROFILE_AVATAR = "profile.avatar";
    /**
     * RESUME。
     */
    public static final String PROFILE_RESUME = "profile.resume";

    /**
     * LOGO。
     */
    public static final String SITE_LOGO = "site.logo";
    /**
     * FAVICON。
     */
    public static final String SITE_FAVICON = "site.favicon";

    /**
     * CHARS。
     */
    private static final Set<Character> EXTRA_PREFIX_CHARS = Set.of('-', '_');

    /**
     * 构造MediaUsageCodes，接收本类使用的依赖或初始化参数。
     */
    private MediaUsageCodes() {
    }

    // usageCode 前缀必须与 sourceModule 一致，避免把 blog.cover 挂到 TUTORIAL 上
    /**
     * 判断是否匹配来源Module。
     * 关键处理步骤调用 <code>sourceModule.trim</code>、<code>usageCode.trim</code>。
     *
     * @param usageCode usage代码；类型为 <code>String</code>
     * @param sourceModule 引用或数据来源所属模块；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    public static boolean matchesSourceModule(String usageCode, String sourceModule) {
        if (usageCode == null || sourceModule == null) {
            return false;
        }
        String expectedPrefix = sourceModule.trim().toLowerCase(Locale.ROOT);
        return usageCode.trim().toLowerCase(Locale.ROOT).startsWith(expectedPrefix + ".");
    }

    // 形如 blog.cover：一段小写前缀 + 一段用途，用途段允许再带点号与连字符
    /**
     * 判断是否具备有效格式。
     * 关键处理步骤调用 <code>usageCode.trim</code>、<code>value.indexOf</code>、<code>value.length</code>、<code>value.substring</code>。
     *
     * @param usageCode usage代码；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
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

    /**
     * 判断是否Segment。
     * 主要协作调用：<code>EXTRA_PREFIX_CHARS.contains</code>。
     *
     * @param segment segment；类型为 <code>String</code>
     * @param allowDots allowDots；类型为 <code>boolean</code>
     * @return 本方法条件是否成立的判断结果
     */
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
