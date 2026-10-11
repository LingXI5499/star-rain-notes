/*
 * SecurityRuleCoverageTest：应用启动模块的测试文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：SecurityRulePlan、ModuleSecurityContributor。
 * 阅读入口：everyContributorDeclaresAtLeastOnePattern（every贡献者Declares时间Least单次模式处理入口）；everyPatternIsAnAbsoluteAntPattern（every模式是否AnAbsoluteAnt模式处理入口）；noPatternIsClaimedAtTwoDifferentAccessLevels（不含模式是否Claimed时间TwoDifferent访问Levels处理入口）；effectiveAccessLevelOfCriticalRoutes（effective访问Level所属Critical路由集合处理入口）。
 */

package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.account.security.SecurityRulePlan;
import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

/*
 * 全模块安全规则表的覆盖率验证。
 *
 * 为什么需要它：第一轮修掉的那个问题——「/api/admin/accounts/{id}/roles 只靠三段 for 循环的书写顺序
 * 才被拒绝」——的根因是**授权语义没有单一真源、也没有测试盯着**。SecurityRulePlan 解决了真源问题，
 * SecurityRulePlanTest 盯住了排序，但那份测试用的是手写的假贡献者。
 *
 * 这个测试打的是另一半：**把真实模块声明的规则全部装进来**，然后验证
 *   1. 不变量（每条模式以 / 开头、每个模块都真的声明了东西、同一模式不会出现互相矛盾的级别）；
 *   2. 关键路由的**生效访问级别**——按顺序第一条命中的规则说了算，这正是 Spring Security 的行为；
 *   3. 没被任何规则覆盖的路径会落到 anyRequest().denyAll()（默认拒绝，不是默认放行）。
 *
 * 不启动 Spring：贡献者都是无参构造，直接反射实例化即可。新增贡献者若带了构造依赖，
 * 这个测试会失败并明确报出是哪一个，那时再改成上下文装配也不迟。
 */
/**
 * 安全规则Coverage测试，属于应用启动模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class SecurityRuleCoverageTest {

    /**
     * MATCHER。
     */
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /**
     * every贡献者Declares时间Least单次模式处理入口。
     * 关键处理步骤调用 <code>contributors</code>、<code>contributor.publicPatterns</code>、<code>contributor.authenticatedPatterns</code>、<code>contributor.deniedPatterns</code>。
     */
    @Test
    void everyContributorDeclaresAtLeastOnePattern() {
        List<String> silent = new ArrayList<>();
        for (ModuleSecurityContributor contributor : contributors()) {
            int declared = contributor.publicPatterns().size()
                    + contributor.authenticatedPatterns().size()
                    + contributor.deniedPatterns().size();
            if (declared == 0) silent.add(contributor.moduleName());
        }
        assertTrue(silent.isEmpty(), "这些模块声明了安全贡献者却一条规则都没有，属于死贡献者：" + silent);
    }

    /**
     * every模式是否AnAbsoluteAnt模式处理入口。
     * 关键处理步骤调用 <code>contributors</code>、<code>allPatterns</code>、<code>pattern.startsWith</code>、<code>problems.add</code>。
     */
    @Test
    void everyPatternIsAnAbsoluteAntPattern() {
        List<String> problems = new ArrayList<>();
        for (ModuleSecurityContributor contributor : contributors()) {
            for (String pattern : allPatterns(contributor)) {
                if (pattern == null || !pattern.startsWith("/")) {
                    problems.add(contributor.moduleName() + " → " + pattern);
                }
            }
        }
        assertTrue(problems.isEmpty(), "URL 模式必须以 / 开头：" + problems);
    }

    /*
     * 同一模式在不同模块被声明成不同访问级别，是真正的安全缺陷：
     * 谁先谁后决定结果，而两边都认为自己说了算。必须启动即失败。
     */
    /**
     * 不含模式是否Claimed时间TwoDifferent访问Levels处理入口。
     * 关键处理步骤调用 <code>contributors</code>、<code>contributor.deniedPatterns</code>、<code>claims.computeIfAbsent</code>、<code>contributor.moduleName</code>。
     */
    @Test
    void noPatternIsClaimedAtTwoDifferentAccessLevels() {
        Map<String, Map<SecurityRulePlan.Access, String>> claims = new TreeMap<>();
        for (ModuleSecurityContributor contributor : contributors()) {
            for (String pattern : contributor.deniedPatterns()) {
                claims.computeIfAbsent(pattern, key -> new LinkedHashMap<>())
                        .put(SecurityRulePlan.Access.DENIED, contributor.moduleName());
            }
            for (String pattern : contributor.publicPatterns()) {
                claims.computeIfAbsent(pattern, key -> new LinkedHashMap<>())
                        .put(SecurityRulePlan.Access.PUBLIC, contributor.moduleName());
            }
            for (String pattern : contributor.authenticatedPatterns()) {
                claims.computeIfAbsent(pattern, key -> new LinkedHashMap<>())
                        .put(SecurityRulePlan.Access.AUTHENTICATED, contributor.moduleName());
            }
        }
        List<String> conflicts = new ArrayList<>();
        claims.forEach((pattern, byAccess) -> {
            if (byAccess.size() > 1) conflicts.add(pattern + " 被同时声明为 " + byAccess);
        });
        assertTrue(conflicts.isEmpty(), "同一个 URL 模式被声明成多个访问级别：\n  " + String.join("\n  ", conflicts));
    }

    /*
     * 关键路由的生效访问级别。这张表就是「谁能匿名访问什么」的可执行版本——
     * 任何一次规则调整只要改变了下列任一结论，这里就会红。
     */
    /**
     * effective访问Level所属Critical路由集合处理入口。
     * 关键处理步骤调用 <code>plannedRules</code>、<code>expected.put</code>、<code>expected.forEach</code>、<code>effective</code>。
     */
    @Test
    void effectiveAccessLevelOfCriticalRoutes() {
        List<SecurityRulePlan.Rule> rules = plannedRules();
        Map<String, SecurityRulePlan.Access> expected = new LinkedHashMap<>();
        // 第一轮修掉的那个端点：旧角色配置接口必须被显式拒绝，且要盖过下面的 /api/admin/accounts/**
        expected.put("/api/admin/accounts/1/roles", SecurityRulePlan.Access.DENIED);
        expected.put("/api/admin/accounts", SecurityRulePlan.Access.AUTHENTICATED);
        expected.put("/api/admin/accounts/1", SecurityRulePlan.Access.AUTHENTICATED);
        expected.put("/api/admin/analytics/summary", SecurityRulePlan.Access.AUTHENTICATED);
        expected.put("/api/admin/site/home-sections", SecurityRulePlan.Access.AUTHENTICATED);
        expected.put("/api/account/learning/plans", SecurityRulePlan.Access.AUTHENTICATED);
        expected.put("/api/account/english/vocabulary/settings", SecurityRulePlan.Access.AUTHENTICATED);
        expected.put("/api/public/site/config", SecurityRulePlan.Access.PUBLIC);
        expected.put("/api/public/tutorials/c-language", SecurityRulePlan.Access.PUBLIC);
        expected.put("/api/public/english/vocabulary/words", SecurityRulePlan.Access.PUBLIC);
        expected.put("/api/public/blog/posts", SecurityRulePlan.Access.PUBLIC);

        List<String> problems = new ArrayList<>();
        expected.forEach((route, access) -> {
            SecurityRulePlan.Access actual = effective(rules, route);
            if (actual != access) {
                problems.add(route + " 期望 " + access + " 实际 " + actual);
            }
        });
        assertTrue(problems.isEmpty(), "关键路由的生效访问级别与预期不符：\n  " + String.join("\n  ", problems));
    }

    /* 没有被任何规则覆盖的路径必须落到 anyRequest().denyAll()，而不是被放行。 */
    /**
     * unmatchedPathsFallThrough目标Deny全部处理入口。
     * 关键处理步骤调用 <code>plannedRules</code>、<code>effective</code>。
     */
    @Test
    void unmatchedPathsFallThroughToDenyAll() {
        List<SecurityRulePlan.Rule> rules = plannedRules();
        assertEquals(null, effective(rules, "/internal/definitely-not-declared"),
                "未声明的路径不得被任何规则放行，必须交给 anyRequest().denyAll()");
        assertEquals(null, effective(rules, "/api/admin/unknown-module/anything"),
                "未声明的模块路径不得被任何规则放行");
    }

    /*
     * denied 是「挖空」而不是「整段封死」：/api/admin/accounts/{id}/roles 被拒，
     * 但更深的子路径仍会落回 /api/admin/accounts/** 的 AUTHENTICATED。
     * 这是有意的——URL 规则只管「能不能进来」，对象级与权限级判断由 @PreAuthorize 负责，
     * 所以这里把实际行为钉住，避免以后有人以为它是整段封禁。
     */
    /**
     * 被拒绝CarveOut是否NarrowerThanTheAllow规则ThatCoversIt处理入口。
     * 关键处理步骤调用 <code>plannedRules</code>、<code>effective</code>。
     */
    @Test
    void deniedCarveOutIsNarrowerThanTheAllowRuleThatCoversIt() {
        List<SecurityRulePlan.Rule> rules = plannedRules();
        assertEquals(SecurityRulePlan.Access.DENIED,
                effective(rules, "/api/admin/accounts/1/roles"));
        assertEquals(SecurityRulePlan.Access.AUTHENTICATED,
                effective(rules, "/api/admin/accounts/1/roles/extra"),
                "更深的子路径应落回 /api/admin/accounts/**，而不是被整段封禁");
    }

    /*
     * 排序不变量：denied 必须全部排在 public 之前，public 必须全部排在 authenticated 之前。
     * SecurityRulePlanTest 用手写假贡献者验过一次，这里用**真实模块的规则**再验一次。
     */
    /**
     * 被拒绝Precedes公开Precedes已认证内部TheReal规则List处理入口。
     * 关键处理步骤调用 <code>plannedRules</code>、<code>rules.size</code>、<code>rules.get</code>、<code>Math.min</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     */
    @Test
    void deniedPrecedesPublicPrecedesAuthenticatedInTheRealRuleList() {
        List<SecurityRulePlan.Rule> rules = plannedRules();
        int lastDenied = -1;
        int lastPublic = -1;
        int firstPublic = rules.size();
        int firstAuthenticated = rules.size();
        for (int index = 0; index < rules.size(); index++) {
            switch (rules.get(index).getAccess()) {
                case DENIED -> lastDenied = index;
                case PUBLIC -> {
                    lastPublic = index;
                    firstPublic = Math.min(firstPublic, index);
                }
                case AUTHENTICATED -> firstAuthenticated = Math.min(firstAuthenticated, index);
                default -> throw new IllegalStateException("未知访问级别");
            }
        }
        assertTrue(lastDenied < firstPublic, "存在排在 public 之后的 denied 规则，收窄会被宽泛规则吃掉");
        assertTrue(lastPublic < firstAuthenticated, "存在排在 authenticated 之后的 public 规则");
    }

    /* 贡献者数量与源码树一致：新增模块却忘了纳入验证范围会在这里失败。 */
    /**
     * 贡献者InventoryMatchesThe来源Tree处理入口。
     * 关键处理步骤调用 <code>contributors</code>、<code>discoverContributorClassNames</code>。
     */
    @Test
    void contributorInventoryMatchesTheSourceTree() {
        assertNotNull(contributors());
        assertEquals(discoverContributorClassNames().size(), contributors().size(),
                "反射实例化出的贡献者数量与源码树扫描结果不一致：" + discoverContributorClassNames());
    }

    // ------------------------------------------------------------------

    /**
     * contributors处理入口。
     * 关键处理步骤调用 <code>discoverContributorClassNames</code>、<code>Class.forName</code>、<code>type.getDeclaredConstructor</code>、<code>constructor.setAccessible</code>。
     * 实现中的显式失败类型包括 <code>AssertionError</code>；具体触发条件见对应分支。
     * @return 本次处理得到的条目集合
     */
    private static List<ModuleSecurityContributor> contributors() {
        List<ModuleSecurityContributor> result = new ArrayList<>();
        for (String className : discoverContributorClassNames()) {
            try {
                Class<?> type = Class.forName(className);
                Constructor<?> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                result.add((ModuleSecurityContributor) constructor.newInstance());
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError("无法无参实例化安全贡献者 " + className
                        + "（如果它新增了构造依赖，需要把这个测试改成从 Spring 上下文取）：" + failure, failure);
            }
        }
        return result;
    }

    /**
     * planned规则处理入口。
     * 关键处理步骤调用 <code>contributors</code>、<code>ordered.sort</code>、<code>Integer.compare</code>、<code>left.order</code>。
     * @return 本次处理得到的条目集合
     */
    private static List<SecurityRulePlan.Rule> plannedRules() {
        List<ModuleSecurityContributor> ordered = new ArrayList<>(contributors());
        ordered.sort((left, right) -> Integer.compare(left.order(), right.order()));
        List<SecurityRulePlan.Rule> rules = SecurityRulePlan.of(ordered);
        assertTrue(!rules.isEmpty(), "规则计划是空的");
        return rules;
    }

    /**
     * effective处理入口。
     * 主要协作调用：<code>MATCHER.match</code>。
     *
     * @param rules 规则；类型为 <code>List&lt;SecurityRulePlan.Rule&gt;</code>
     * @param path 路径；类型为 <code>String</code>
     * @return 类型为 <code>SecurityRulePlan.Access</code> 的处理结果
     */
    private static SecurityRulePlan.Access effective(List<SecurityRulePlan.Rule> rules, String path) {
        for (SecurityRulePlan.Rule rule : rules) {
            if (MATCHER.match(rule.getPattern(), path)) return rule.getAccess();
        }
        return null;
    }

    /**
     * 全部Patterns处理入口。
     * 关键处理步骤调用 <code>patterns.addAll</code>、<code>contributor.deniedPatterns</code>、<code>contributor.publicPatterns</code>、<code>contributor.authenticatedPatterns</code>。
     *
     * @param contributor 贡献者；类型为 <code>ModuleSecurityContributor</code>
     * @return 本次处理得到的条目集合
     */
    private static List<String> allPatterns(ModuleSecurityContributor contributor) {
        List<String> patterns = new ArrayList<>();
        patterns.addAll(contributor.deniedPatterns());
        patterns.addAll(contributor.publicPatterns());
        patterns.addAll(contributor.authenticatedPatterns());
        return patterns;
    }

    /**
     * discover贡献者ClassNames处理入口。
     * 关键处理步骤调用 <code>backendRoot</code>、<code>Files.list</code>、<code>modules.filter</code>、<code>module.resolve</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     * @return 本次处理得到的条目集合
     */
    private static List<String> discoverContributorClassNames() {
        Path backend = backendRoot();
        List<String> names = new ArrayList<>();
        try (Stream<Path> modules = Files.list(backend)) {
            for (Path module : modules.filter(Files::isDirectory).toList()) {
                Path javaRoot = module.resolve("src/main/java");
                if (!Files.isDirectory(javaRoot)) continue;
                try (Stream<Path> sources = Files.walk(javaRoot)) {
                    for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                        String text = Files.readString(source, StandardCharsets.UTF_8);
                        if (!text.contains("implements ModuleSecurityContributor")) continue;
                        names.add(javaRoot.relativize(source).toString()
                                .replace('\\', '.').replace('/', '.')
                                .replaceAll("\\.java$", ""));
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("扫描安全贡献者失败", exception);
        }
        names.sort(String::compareTo);
        return names;
    }

    /**
     * backendRoot处理入口。
     * 关键处理步骤调用 <code>Path.of</code>、<code>current.getFileName</code>、<code>current.getParent</code>。
     * @return 类型为 <code>Path</code> 的处理结果
     */
    private static Path backendRoot() {
        Path current = Path.of("").toAbsolutePath();
        if (current.getFileName() != null && current.getFileName().toString().startsWith("star-rain-")) {
            return current.getParent();
        }
        return current;
    }
}
