package com.starrainnotes.account.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.starrainnotes.common.context.HostEntryResolver;
import com.starrainnotes.common.properties.PlatformProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

/*
 * 注册端点入口限制的匹配器测试。
 *
 * 为什么不搭完整安全链：
 *   这里要验证的是「哪些请求会被这条规则命中」这一件事，
 *   完整链会把 CSRF、会话、方法级安全一起拉进来，噪音大且依赖数据库。
 *   直接对 RequestMatcher 断言既精确又不需要基础设施；规则在链上的位置由
 *   AccountSecurityConfig 的顺序（denyAll 组，permitAll 之前）保证。
 *
 * matches() 返回 true 的含义是「命中 denyAll 规则」＝ 该请求会被拒绝。
 */
class RegistrationEntryRequestMatcherTest {

    // 默认域名配置与 application.yml 一致
    private final RegistrationEntryRequestMatcher matcher =
            new RegistrationEntryRequestMatcher(new HostEntryResolver(new PlatformProperties()));

    private static MockHttpServletRequest request(String method, String path, String host) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (host != null) {
            request.addHeader("Host", host);
        }
        return request;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1",              // 公开站
            "127.0.0.1:8088",
            "localhost",
            "yulanlin.cn",            // 公开站正式域名
            "www.yulanlin.cn",
            "admin.localhost",        // 管理站
            "admin.yulanlin.cn",
            "unknown.example.com",    // 未知域名按公开站处理
            "user.localhost.evil.com",
    })
    @DisplayName("非用户站的注册请求被命中，从而由 denyAll 拒绝")
    void rejectsRegistrationOutsideUserEntry(String host) {
        assertThat(matcher.matches(request("POST", "/api/auth/register", host))).isTrue();
    }

    @Test
    @DisplayName("用户站的注册请求不被命中，继续走正常业务校验")
    void allowsRegistrationOnUserEntry() {
        assertThat(matcher.matches(request("POST", "/api/auth/register", "user.localhost"))).isFalse();
        assertThat(matcher.matches(request("POST", "/api/auth/register", "user.yulanlin.cn"))).isFalse();
        assertThat(matcher.matches(request("POST", "/api/auth/register", "USER.LOCALHOST:5174"))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "localhost", "admin.localhost", "unknown.example.com"})
    @DisplayName("验证码子路径与注册主路径同一个口径")
    void coversVerificationCodeSubPath(String host) {
        assertThat(matcher.matches(request("POST", "/api/auth/register/verification-codes", host))).isTrue();
    }

    @Test
    @DisplayName("用户站的验证码子路径同样放行")
    void allowsVerificationCodeOnUserEntry() {
        assertThat(matcher.matches(
                request("POST", "/api/auth/register/verification-codes", "user.localhost"))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/csrf",
            "/api/auth/password-reset/request",
            "/api/auth/password-reset/confirm",
    })
    @DisplayName("登录、登出、CSRF、密码重置不在收窄范围内：三个入口都要能用")
    void ignoresOtherAuthEndpoints(String path) {
        assertThat(matcher.matches(request("POST", path, "127.0.0.1"))).isFalse();
        assertThat(matcher.matches(request("POST", path, "admin.localhost"))).isFalse();
        assertThat(matcher.matches(request("POST", path, "unknown.example.com"))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/account/me",
            "/api/admin/accounts",
            "/api/admin/media/assets",
            "/api/auth/register-foo",
            "/api/auth/registrations",
    })
    @DisplayName("非注册类路径一律不命中，避免规则外溢")
    void ignoresNonRegistrationPaths(String path) {
        assertThat(matcher.matches(request("POST", path, "127.0.0.1"))).isFalse();
        assertThat(matcher.matches(request("POST", path, "admin.localhost"))).isFalse();
    }

    @Test
    @DisplayName("没有 Host 头时退回 serverName 判定")
    void fallsBackToServerNameWhenHostHeaderMissing() {
        MockHttpServletRequest adminRequest = request("POST", "/api/auth/register", null);
        adminRequest.setServerName("admin.localhost");
        assertThat(matcher.matches(adminRequest)).isTrue();

        MockHttpServletRequest userRequest = request("POST", "/api/auth/register", null);
        userRequest.setServerName("user.localhost");
        assertThat(matcher.matches(userRequest)).isFalse();
    }

    @Test
    @DisplayName("Host 头与 serverName 都不可用/不认识时按公开站处理，即拒绝注册")
    void unknownOrMissingHostIsRejected() {
        // MockHttpServletRequest 默认 serverName 是 localhost，属于公开站
        assertThat(matcher.matches(request("POST", "/api/auth/register", null))).isTrue();

        MockHttpServletRequest blankHost = request("POST", "/api/auth/register", "   ");
        blankHost.setServerName("completely-unknown.example.net");
        assertThat(matcher.matches(blankHost)).isTrue();
    }

    @Test
    @DisplayName("GET 等其它方法同样受这条规则约束（规则只按路径与 Host 判定）")
    void appliesRegardlessOfHttpMethod() {
        assertThat(matcher.matches(request("GET", "/api/auth/register", "admin.localhost"))).isTrue();
        assertThat(matcher.matches(request("GET", "/api/auth/register", "user.localhost"))).isFalse();
    }
}
