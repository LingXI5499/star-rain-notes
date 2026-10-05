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
class SecurityRulePlanTest {

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

    @Test
    void publicRulesAreAlwaysPlannedBeforeAuthenticatedRules() {
        List<Rule> rules = SecurityRulePlan.of(List.of(
                contributor("account", List.of(), List.of("/api/auth/login"), List.of("/api/account/**"))));

        assertEquals(List.of(Access.PUBLIC, Access.AUTHENTICATED),
                rules.stream().map(Rule::getAccess).toList());
    }

    @Test
    void narrowerDeniedPatternMayCarveOutABroaderAuthenticatedTree() {
        SecurityPatternValidator.validate(List.of(
                contributor("account", List.of("/api/admin/accounts/*/roles"), List.of(),
                        List.of("/api/admin/accounts/**"))));
    }

    @Test
    void deniedPatternOfEqualWidthIsRejectedEvenIfItLooksLikeACarveOut() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> SecurityPatternValidator.validate(List.of(
                contributor("account", List.of("/api/admin/accounts/**"), List.of(),
                        List.of("/api/admin/accounts/**")))));

        assertTrue(error.getMessage().contains("denied/authenticated"));
    }

    @Test
    void broaderDeniedPatternThatMakesAGrantDeadIsRejected() {
        assertThrows(IllegalStateException.class, () -> SecurityPatternValidator.validate(List.of(
                contributor("account", List.of("/api/admin/**"), List.of(), List.of("/api/admin/accounts/**")))));
    }

    @Test
    void publicAndAuthenticatedOverlapIsStillRejected() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> SecurityPatternValidator.validate(List.of(
                contributor("account", List.of(), List.of("/api/blog/**"), List.of("/api/blog/posts")))));

        assertTrue(error.getMessage().contains("public/authenticated"));
    }

    private ModuleSecurityContributor contributor(String name, List<String> denied,
                                                  List<String> publicPatterns, List<String> authenticated) {
        return new ModuleSecurityContributor() {
            @Override
            public String moduleName() {
                return name;
            }

            @Override
            public List<String> deniedPatterns() {
                return denied;
            }

            @Override
            public List<String> publicPatterns() {
                return publicPatterns;
            }

            @Override
            public List<String> authenticatedPatterns() {
                return authenticated;
            }
        };
    }
}
