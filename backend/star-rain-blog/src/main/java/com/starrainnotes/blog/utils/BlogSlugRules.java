/*
 * BlogSlugRules：博客模块的工具文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 阅读入口：normalize（规范化博客固定链接标识）；isValid（判断是否有效）。
 */

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
/**
 * 博客固定链接标识规则，属于博客模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class BlogSlugRules {

    /**
     * 模式。
     */
    private static final String SLUG_PATTERN = "[a-z0-9]+(?:-[a-z0-9]+)*";

    /**
     * 构造BlogSlugRules，接收本类使用的依赖或初始化参数。
     */
    private BlogSlugRules() {
    }

    // 去掉首尾空白并转小写；null / 空白返回 null，由调用方决定默认值
    /**
     * 规范化博客固定链接标识。
     * 关键处理步骤调用 <code>value.trim</code>、<code>trimmed.isEmpty</code>、<code>trimmed.toLowerCase</code>。
     * 边界判断包含 <code>(value == null)</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    // 规范化之后再判断，保证 "Hello-World" 这种输入是被接受而不是被拒绝
    /**
     * 判断是否有效。
     * 关键处理步骤调用 <code>normalized.length</code>、<code>normalized.matches</code>。
     * 边界判断包含 <code>(normalized == null || normalized.length() &gt; maxLength)</code>。
     *
     * @param normalized 规范化；类型为 <code>String</code>
     * @param maxLength 最大Length；类型为 <code>int</code>
     * @return 本方法条件是否成立的判断结果
     */
    public static boolean isValid(String normalized, int maxLength) {
        if (normalized == null || normalized.length() > maxLength) {
            return false;
        }
        return normalized.matches(SLUG_PATTERN);
    }
}
