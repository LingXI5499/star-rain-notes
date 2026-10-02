package com.starrainnotes.common.context;

import com.starrainnotes.common.enumeration.SiteEntry;
import com.starrainnotes.common.properties.PlatformProperties;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/*
 * 由 Host 字符串解析站点入口。
 *
 * 无状态、只依赖配置，因此可以直接 new 出来做单元测试，不需要起 Spring 上下文。
 *
 * 匹配规则（必须与前端 frontend/src/shared/entry.js 保持一致）：
 *   1. 忽略大小写、忽略端口（"User.Localhost:5174" 与 "user.localhost" 等价）；
 *   2. 支持 "*.example.com" 形式的通配后缀，并且要求通配符至少吃掉一级标签：
 *      "*.yulanlin.cn" 命中 "user.yulanlin.cn" 与 "a.b.yulanlin.cn"，不命中 "yulanlin.cn"；
 *      比较时保留前导点，因此 "notyulanlin.cn" 也不会被误命中；
 *   3. null、空串、空白串、未配置的域名，一律落到 PUBLIC。
 *
 * 第 3 条是权限最小化，也是这个类存在的理由：
 * 宁可把管理站域名写错导致超管进不去（立刻就能发现），
 * 也不能把未知域名当作用户站或管理站，否则任何指向本服务的域名都能拿到注册能力。
 *
 * 配置冲突（同一域名同时出现在多个列表）时按 PUBLIC → USER → ADMIN 遍历取第一个命中的，
 * 同样取权限最小的一侧。
 */
@Component
public class HostEntryResolver {

    private final PlatformProperties properties;

    public HostEntryResolver(PlatformProperties properties) {
        this.properties = properties;
    }

    // 解析入口；入参可以是 Host 头的原样内容，也可以是不带端口的 serverName
    public SiteEntry resolve(String host) {
        String normalizedHost = normalizeHost(host);
        if (normalizedHost.isEmpty()) {
            return SiteEntry.PUBLIC;
        }
        for (SiteEntry entry : SiteEntry.values()) {
            if (matchesAny(properties.hostsOf(entry), normalizedHost)) {
                return entry;
            }
        }
        return SiteEntry.PUBLIC;
    }

    /*
     * 归一化：去空白、转小写、去端口。
     * 唯一需要特殊处理的是 IPv6 字面量：带方括号时端口在括号外（[::1]:8088），
     * 不带方括号时冒号本身就是地址的一部分，不能当成端口截断。
     */
    private static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String trimmed = host.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.startsWith("[")) {
            int closingBracket = trimmed.indexOf(']');
            return closingBracket > 0
                    ? trimmed.substring(1, closingBracket)
                    : trimmed.substring(1);
        }
        int firstColon = trimmed.indexOf(':');
        if (firstColon < 0) {
            return trimmed;
        }
        // 出现第二个冒号说明是无方括号的 IPv6 地址，整体保留
        if (trimmed.indexOf(':', firstColon + 1) >= 0) {
            return trimmed;
        }
        return trimmed.substring(0, firstColon);
    }

    private static boolean matchesAny(List<String> patterns, String host) {
        if (patterns == null) {
            return false;
        }
        for (String pattern : patterns) {
            if (matches(pattern, host)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String pattern, String host) {
        if (pattern == null) {
            return false;
        }
        String candidate = pattern.trim().toLowerCase(Locale.ROOT);
        if (candidate.isEmpty()) {
            return false;
        }
        if (!candidate.startsWith("*.")) {
            return candidate.equals(host);
        }
        // 保留 "*.example.com" 里的前导点，避免 "notexample.com" 这类后缀相同的域名被命中
        String suffix = candidate.substring(1);
        return host.length() > suffix.length() && host.endsWith(suffix);
    }
}
