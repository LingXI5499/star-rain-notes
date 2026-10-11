/*
 * SecurityRulePlanTest：账号模块的测试文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ModuleSecurityContributor、Access、Rule。
 * 阅读入口：moduleName（module名称处理入口）；deniedPatterns（被拒绝Patterns处理入口）；publicPatterns（公开Patterns处理入口）；authenticatedPatterns（已认证Patterns处理入口）。
 */

package com.starrainnotes.account.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import com.starrainnotes.account.security.SecurityRulePlan.Access;
import com.starrainnotes.account.security.SecurityRulePlan.Rule;
import java.util.List;
import org.junit.jupiter.api.Test;

/*
 * 这个测试钉住的是「安全的隐式前提」本身，而不是某个端点的行为。
 *
 * 背景：Account 同时声明 denied `/api/admin/accounts/* /roles` 与
 * authenticated `/api/admin/accounts/**`。它之所以安全，靠的就是 denied 规则先下发。
 * 这个顺序曾经只存在于 SecurityFilterChain 里三段 for 循环的书写顺序中 ——
 * 重排它们不会让任何测试失败，却会让该端点变成任何已登录用户可访问。
 * 现在顺序由 SecurityRulePlan.of() 唯一决定，并由本文件断言。
 */
/**
 * 安全规则计划测试，属于账号模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class SecurityRulePlanTest {

    /**
     * 被拒绝规则AreAlwaysPlanned之前公开及已认证规则处理入口。
     * 关键处理步骤调用 <code>SecurityRulePlan.of</code>、<code>List.of</code>、<code>contributor</code>、<code>rules.stream</code>。
     */
    @Test
    void deniedRulesAreAlwaysPlannedBeforePublicAndAuthenticatedRules() {
        List<Rule> rules = SecurityRulePlan.of(List.of(
                contributor("wide", List.of(), List.of("/api/public/**"), List.of("/api/admin/**")),
                contributor("narrow", List.of("/api/admin/accounts/*/roles"), List.of(), List.of())));

        assertEquals(List.of(Access.DENIED, Access.PUBLIC, Access.AUTHENTICATED),
                rules.stream().map(Rule::getAccess).toList());
        assertEquals("/api/admin/accounts/*/roles", rules.get(0).getPattern());
        assertEquals("narrow", rules.get(0).getModule());
    }

    /**
     * 公开规则AreAlwaysPlanned之前已认证规则处理入口。
     * 关键处理步骤调用 <code>SecurityRulePlan.of</code>、<code>List.of</code>、<code>contributor</code>、<code>rules.stream</code>。
     */
    @Test
    void publicRulesAreAlwaysPlannedBeforeAuthenticatedRules() {
        List<Rule> rules = SecurityRulePlan.of(List.of(
                contributor("account", List.of(), List.of("/api/auth/login"), List.of("/api/account/**"))));

        assertEquals(List.of(Access.PUBLIC, Access.AUTHENTICATED),
                rules.stream().map(Rule::getAccess).toList());
    }

    /**
     * narrower被拒绝模式MayCarveOutABroader已认证Tree处理入口。
     * 关键处理步骤调用 <code>SecurityPatternValidator.validate</code>、<code>List.of</code>、<code>contributor</code>。
     */
    @Test
    void narrowerDeniedPatternMayCarveOutABroaderAuthenticatedTree() {
        SecurityPatternValidator.validate(List.of(
                contributor("account", List.of("/api/admin/accounts/*/roles"), List.of(),
                        List.of("/api/admin/accounts/**"))));
    }

    /**
     * 被拒绝模式所属Equal宽度是否已拒绝EvenIfItLooksLikeACarveOut处理入口。
     * 关键处理步骤调用 <code>SecurityPatternValidator.validate</code>、<code>List.of</code>、<code>contributor</code>、<code>error.getMessage</code>。
     */
    @Test
    void deniedPatternOfEqualWidthIsRejectedEvenIfItLooksLikeACarveOut() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> SecurityPatternValidator.validate(List.of(
                contributor("account", List.of("/api/admin/accounts/**"), List.of(),
                        List.of("/api/admin/accounts/**")))));

        assertTrue(error.getMessage().contains("denied/authenticated"));
    }

    /**
     * broader被拒绝模式ThatMakesA授权Dead是否已拒绝处理入口。
     * 关键处理步骤调用 <code>SecurityPatternValidator.validate</code>、<code>List.of</code>、<code>contributor</code>。
     */
    @Test
    void broaderDeniedPatternThatMakesAGrantDeadIsRejected() {
        assertThrows(IllegalStateException.class, () -> SecurityPatternValidator.validate(List.of(
                contributor("account", List.of("/api/admin/**"), List.of(), List.of("/api/admin/accounts/**")))));
    }

    /**
     * 公开及已认证Overlap是否Still已拒绝处理入口。
     * 关键处理步骤调用 <code>SecurityPatternValidator.validate</code>、<code>List.of</code>、<code>contributor</code>、<code>error.getMessage</code>。
     */
    @Test
    void publicAndAuthenticatedOverlapIsStillRejected() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> SecurityPatternValidator.validate(List.of(
                contributor("account", List.of(), List.of("/api/blog/**"), List.of("/api/blog/posts")))));

        assertTrue(error.getMessage().contains("public/authenticated"));
    }

    /**
     * 贡献者处理入口。
     *
     * @param name 名称；类型为 <code>String</code>
     * @param denied 被拒绝；类型为 <code>List&lt;String&gt;</code>
     * @param publicPatterns 公开Patterns；类型为 <code>List&lt;String&gt;</code>
     * @param authenticated 已认证；类型为 <code>List&lt;String&gt;</code>
     * @return 类型为 <code>ModuleSecurityContributor</code> 的处理结果
     */
    private ModuleSecurityContributor contributor(String name, List<String> denied,
                                                  List<String> publicPatterns, List<String> authenticated) {
        return new ModuleSecurityContributor()
    /**
     * 当前调用使用的匿名协作实现，属于账号模块。
     * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
     */
    {
            /**
             * module名称处理入口。
             * @return 类型为 <code>String</code> 的处理结果
             */
            @Override
            public String moduleName() {
                return name;
            }

            /**
             * 被拒绝Patterns处理入口。
             * @return 本次处理得到的条目集合
             */
            @Override
            public List<String> deniedPatterns() {
                return denied;
            }

            /**
             * 公开Patterns处理入口。
             * @return 本次处理得到的条目集合
             */
            @Override
            public List<String> publicPatterns() {
                return publicPatterns;
            }

            /**
             * 已认证Patterns处理入口。
             * @return 本次处理得到的条目集合
             */
            @Override
            public List<String> authenticatedPatterns() {
                return authenticated;
            }
        };
    }
}
