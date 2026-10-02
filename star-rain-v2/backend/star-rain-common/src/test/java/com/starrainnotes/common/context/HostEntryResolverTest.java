package com.starrainnotes.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.starrainnotes.common.enumeration.SiteEntry;
import com.starrainnotes.common.properties.PlatformProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/*
 * Host → 入口解析测试。
 *
 * 这是三入口隔离的地基：解析错一次，就可能让公开站域名拿到注册能力，
 * 因此这里把「带端口 / 大写 / 通配 / 未知 / 空」逐条钉死。
 * 用例全部直接 new 解析器，不起 Spring 上下文。
 */
class HostEntryResolverTest {

    private static HostEntryResolver resolverOf(List<String> publicHosts,
                                               List<String> userHosts,
                                               List<String> adminHosts) {
        PlatformProperties properties = new PlatformProperties();
        properties.setPublicHosts(publicHosts);
        properties.setUserHosts(userHosts);
        properties.setAdminHosts(adminHosts);
        return new HostEntryResolver(properties);
    }

    // 默认配置＝application.yml 的默认值，绝大多数用例都基于它
    private static HostEntryResolver defaultResolver() {
        return resolverOf(
                List.of("yulanlin.cn", "www.yulanlin.cn", "127.0.0.1", "localhost"),
                List.of("user.yulanlin.cn", "user.localhost"),
                List.of("admin.yulanlin.cn", "admin.localhost"));
    }

    @ParameterizedTest
    @CsvSource({
            "yulanlin.cn, PUBLIC",
            "www.yulanlin.cn, PUBLIC",
            "127.0.0.1, PUBLIC",
            "localhost, PUBLIC",
            "user.yulanlin.cn, USER",
            "user.localhost, USER",
            "admin.yulanlin.cn, ADMIN",
            "admin.localhost, ADMIN",
    })
    @DisplayName("各入口的精确域名解析正确")
    void resolvesExactHosts(String host, SiteEntry expected) {
        assertThat(defaultResolver().resolve(host)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "user.localhost:5174, USER",
            "admin.localhost:8088, ADMIN",
            "127.0.0.1:8088, PUBLIC",
            "user.yulanlin.cn:443, USER",
    })
    @DisplayName("带端口的 Host 去掉端口后再匹配")
    void ignoresPort(String host, SiteEntry expected) {
        assertThat(defaultResolver().resolve(host)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "USER.LOCALHOST, USER",
            "User.Localhost, USER",
            "ADMIN.YULANLIN.CN, ADMIN",
            "Admin.Localhost:5174, ADMIN",
            "LOCALHOST, PUBLIC",
    })
    @DisplayName("大写 Host 归一化后匹配")
    void ignoresCase(String host, SiteEntry expected) {
        assertThat(defaultResolver().resolve(host)).isEqualTo(expected);
    }

    @Test
    @DisplayName("通配后缀命中任意一级或多级子域，但不命中裸域名本身")
    void supportsWildcardSuffix() {
        HostEntryResolver resolver = resolverOf(
                List.of("yulanlin.cn"),
                List.of("*.yulanlin.cn"),
                List.of());

        assertThat(resolver.resolve("user.yulanlin.cn")).isEqualTo(SiteEntry.USER);
        assertThat(resolver.resolve("blog.yulanlin.cn")).isEqualTo(SiteEntry.USER);
        assertThat(resolver.resolve("a.b.yulanlin.cn")).isEqualTo(SiteEntry.USER);
        assertThat(resolver.resolve("USER.YULANLIN.CN:5174")).isEqualTo(SiteEntry.USER);
        // 裸域名由自己的规则命中 PUBLIC，通配规则不该抢走它
        assertThat(resolver.resolve("yulanlin.cn")).isEqualTo(SiteEntry.PUBLIC);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // 后缀相同但不是子域，前后缀匹配必须带上点边界
            "notyulanlin.cn",
            "evilyulanlin.cn",
    })
    @DisplayName("后缀相同的伪装域名不会被通配规则命中")
    void wildcardKeepsLabelBoundary(String host) {
        HostEntryResolver resolver = resolverOf(List.of(), List.of("*.yulanlin.cn"), List.of());

        assertThat(resolver.resolve(host)).isEqualTo(SiteEntry.PUBLIC);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "evil.example.com",
            "yulanlin.cn.evil.com",
            "127.0.0.2",
            "unknown.localhost",
            "user.localhost.evil.com",
    })
    @DisplayName("未知域名一律解析为 PUBLIC")
    void unknownHostsFallBackToPublic(String host) {
        assertThat(defaultResolver().resolve(host)).isEqualTo(SiteEntry.PUBLIC);
    }

    @Test
    @DisplayName("null 与空白 Host 一律解析为 PUBLIC")
    void blankHostsFallBackToPublic() {
        HostEntryResolver resolver = defaultResolver();

        assertThat(resolver.resolve(null)).isEqualTo(SiteEntry.PUBLIC);
        assertThat(resolver.resolve("")).isEqualTo(SiteEntry.PUBLIC);
        assertThat(resolver.resolve("   ")).isEqualTo(SiteEntry.PUBLIC);
        assertThat(resolver.resolve("\t")).isEqualTo(SiteEntry.PUBLIC);
    }

    @Test
    @DisplayName("Host 两侧的空白被忽略")
    void trimsSurroundingWhitespace() {
        assertThat(defaultResolver().resolve("  user.localhost  ")).isEqualTo(SiteEntry.USER);
        assertThat(defaultResolver().resolve("\tadmin.localhost\n")).isEqualTo(SiteEntry.ADMIN);
    }

    @Test
    @DisplayName("配置冲突时取权限最小的一侧")
    void conflictingConfigurationPrefersLeastPrivilege() {
        // 同一域名同时写进公开站与用户站：按 PUBLIC → USER → ADMIN 的顺序取 PUBLIC
        HostEntryResolver publicWins = resolverOf(
                List.of("user.localhost"), List.of("user.localhost"), List.of("user.localhost"));
        assertThat(publicWins.resolve("user.localhost")).isEqualTo(SiteEntry.PUBLIC);

        // 只冲突用户站与管理站时，取 USER
        HostEntryResolver userWins = resolverOf(
                List.of(), List.of("admin.localhost"), List.of("admin.localhost"));
        assertThat(userWins.resolve("admin.localhost")).isEqualTo(SiteEntry.USER);
    }

    @Test
    @DisplayName("配置里混入 null、空白与空列表时不影响其它规则")
    void toleratesDirtyConfiguration() {
        HostEntryResolver resolver = resolverOf(
                java.util.Arrays.asList(null, "  ", "user.localhost"),
                null,
                java.util.Collections.emptyList());

        assertThat(resolver.resolve("user.localhost")).isEqualTo(SiteEntry.PUBLIC);
        assertThat(resolver.resolve("admin.localhost")).isEqualTo(SiteEntry.PUBLIC);
    }

    @Test
    @DisplayName("IPv6 字面量：带方括号的端口被剥离，无方括号的地址整体保留")
    void handlesIpv6Literals() {
        HostEntryResolver resolver = resolverOf(List.of("::1"), List.of(), List.of());

        assertThat(resolver.resolve("[::1]:8088")).isEqualTo(SiteEntry.PUBLIC);
        assertThat(resolver.resolve("::1")).isEqualTo(SiteEntry.PUBLIC);
        // 没有任何规则命中时也必须给出 PUBLIC，而不是抛异常
        assertThat(resolver.resolve("[2001:db8::1]:8088")).isEqualTo(SiteEntry.PUBLIC);
    }

    @Test
    @DisplayName("SiteEntry 的 code 与前端字符串约定一致")
    void entryCodesMatchFrontendContract() {
        assertThat(SiteEntry.PUBLIC.getCode()).isEqualTo("public");
        assertThat(SiteEntry.USER.getCode()).isEqualTo("user");
        assertThat(SiteEntry.ADMIN.getCode()).isEqualTo("admin");
        // 声明顺序即权限顺位，解析器依赖它做「冲突取最小权限」
        assertThat(SiteEntry.values())
                .containsExactly(SiteEntry.PUBLIC, SiteEntry.USER, SiteEntry.ADMIN);
    }
}
