package com.starrainnotes.account.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/*
 * 授权规则计划 —— 把各模块声明的 URL 边界编译成一条**优先级唯一确定**的规则序列。
 *
 * 优先级本身就是安全语义：denied 必须先于 public、public 必须先于 authenticated。
 * 最典型的例子就在 Account 自己身上 —— 它同时声明了
 *   denied         /api/admin/accounts/* /roles   （旧角色配置接口，直接封死）
 *   authenticated  /api/admin/accounts/**          （后台账户管理，只要求已登录）
 * 前者是后者的收窄挖空。若这三类规则以别的顺序进入 Spring Security，
 * 该端点会退化成「任何已登录用户可访问」，而且没有任何测试会失败。
 *
 * 因此顺序不再由 SecurityFilterChain 里三段并列的 for 循环隐式承载 ——
 * 那种写法可以被静默重排；而是由这里的 of() 唯一决定，并由
 * SecurityRulePlanTest 把「DENIED 全部排在 PUBLIC 之前、PUBLIC 全部排在 AUTHENTICATED 之前」钉死。
 */
public final class SecurityRulePlan {
    private SecurityRulePlan() {
    }

    // URL 边界的三种访问级别，声明顺序即落地顺序
    public enum Access {
        DENIED,
        PUBLIC,
        AUTHENTICATED
    }

    /*
     * 一条待下发的授权规则。
     *
     * module 只用于诊断与冲突排查，不参与匹配。
     * 项目约定禁 record，这里用 Lombok POJO 保持与 VO / DTO 一致的 JavaBean 形状。
     */
    @Data
    @AllArgsConstructor
    public static class Rule {
        private String pattern;
        private Access access;
        private String module;
    }

    /**
     * @param orderedContributors 已按 ModuleSecurityContributor.order() 升序排好的贡献者
     * @return denied → public → authenticated 的规则序列，供过滤器链按序下发
     */
    public static List<Rule> of(List<ModuleSecurityContributor> orderedContributors) {
        List<Rule> rules = new ArrayList<>();
        for (Access access : Access.values()) {
            for (ModuleSecurityContributor contributor : orderedContributors) {
                for (String pattern : patternsOf(contributor, access)) {
                    rules.add(new Rule(pattern, access, contributor.moduleName()));
                }
            }
        }
        return List.copyOf(rules);
    }

    private static List<String> patternsOf(ModuleSecurityContributor contributor, Access access) {
        return switch (access) {
            case DENIED -> contributor.deniedPatterns();
            case PUBLIC -> contributor.publicPatterns();
            case AUTHENTICATED -> contributor.authenticatedPatterns();
        };
    }
}
