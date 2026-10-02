package com.starrainnotes.account.security;

import com.starrainnotes.common.context.HostEntryResolver;
import com.starrainnotes.common.enumeration.SiteEntry;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/*
 * 「注册类端点只在用户站开放」的服务端兜底。
 *
 * 为什么需要它：
 *   前端公开站根本不渲染注册入口，但那只是渲染层——任何人手搓一个 POST 就能打到同一台后端。
 *   真正的准入必须由服务端判定，否则「公开站不暴露注册」只是一句界面约定。
 *
 * 为什么是自定义 RequestMatcher：
 *   Spring Security 的 requestMatchers(...) 只按路径匹配，不支持按 Host 匹配。
 *   这里把「路径属于注册类」与「当前 Host 解析出来不是 USER 入口」两个条件合成一个匹配器，
 *   交给 AccountSecurityConfig 放进 denyAll 的一组规则里。
 *
 * 覆盖范围只有注册类端点：/api/auth/register 及其全部子路径
 * （当前是 /register/verification-codes）。登录、登出、CSRF 与密码重置不在收窄范围内，
 * 三个入口都要能用——超管在管理站也需要能登录。
 */
public class RegistrationEntryRequestMatcher implements RequestMatcher {

    /*
     * 注册类路径。
     * "/api/auth/register/**" 在 PathPattern 语义下同时命中 "/api/auth/register" 本身，
     * 这里两条都留着是为了不依赖该语义细节：无论容器怎么归一化，两条里总有一条命中。
     */
    private static final List<RequestMatcher> REGISTRATION_PATHS = List.of(
            PathPatternRequestMatcher.withDefaults().matcher("/api/auth/register"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/auth/register/**"));

    private final HostEntryResolver hostEntryResolver;

    public RegistrationEntryRequestMatcher(HostEntryResolver hostEntryResolver) {
        this.hostEntryResolver = hostEntryResolver;
    }

    // 命中即拒绝：非用户站（公开站、管理站、未知域名）都不能注册
    @Override
    public boolean matches(HttpServletRequest request) {
        if (!isRegistrationPath(request)) {
            return false;
        }
        return hostEntryResolver.resolve(resolveHost(request)) != SiteEntry.USER;
    }

    private static boolean isRegistrationPath(HttpServletRequest request) {
        for (RequestMatcher registrationPath : REGISTRATION_PATHS) {
            if (registrationPath.matches(request)) {
                return true;
            }
        }
        return false;
    }

    /*
     * 优先读 Host 头，其次退回容器解析出的 serverName。
     *
     * 两条路都要留：真实容器可能对 Host 头做归一化（补端口、去掉非法字符），
     * 而 MockHttpServletRequest 不会因为加了 Host 头就改写 serverName，单测依赖前者。
     */
    private static String resolveHost(HttpServletRequest request) {
        String hostHeader = request.getHeader("Host");
        if (hostHeader != null && !hostHeader.isBlank()) {
            return hostHeader;
        }
        return request.getServerName();
    }
}
