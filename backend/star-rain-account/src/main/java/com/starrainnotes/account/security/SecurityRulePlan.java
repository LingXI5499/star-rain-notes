/*
 * SecurityRulePlan：账号模块的安全策略文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 * 主要协作类型：ModuleSecurityContributor。
 * 阅读入口：of（所属处理入口）。
 */

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
/**
 * 安全规则计划，属于账号模块。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 */
public final class SecurityRulePlan {
    /**
     * 构造SecurityRulePlan，接收本类使用的依赖或初始化参数。
     */
    private SecurityRulePlan() {
    }

    // URL 边界的三种访问级别，声明顺序即落地顺序
    /**
     * 访问，属于账号模块。
     * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
     */
    public enum Access {
        /**
         * 被拒绝。
         */
        DENIED,
        /**
         * 公开。
         */
        PUBLIC,
        /**
         * 已认证。
         */
        AUTHENTICATED
    }

    /*
     * 一条待下发的授权规则。
     *
     * module 只用于诊断与冲突排查，不参与匹配。
     * 项目约定禁 record，这里用 Lombok POJO 保持与 VO / DTO 一致的 JavaBean 形状。
     */
    /**
     * 规则，属于账号模块。
     * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
     */
    @Data
    @AllArgsConstructor
    public static class Rule {
        /**
         * 模式。
         */
        private String pattern;
        /**
         * 访问。
         */
        private Access access;
        /**
         * module。
         */
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

    /**
     * patterns所属处理入口。
     * 关键处理步骤调用 <code>contributor.deniedPatterns</code>、<code>contributor.publicPatterns</code>、<code>contributor.authenticatedPatterns</code>。
     *
     * @param contributor 贡献者；类型为 <code>ModuleSecurityContributor</code>
     * @param access 访问；类型为 <code>Access</code>
     * @return 本次处理得到的条目集合
     */
    private static List<String> patternsOf(ModuleSecurityContributor contributor, Access access) {
        return switch (access) {
            case DENIED -> contributor.deniedPatterns();
            case PUBLIC -> contributor.publicPatterns();
            case AUTHENTICATED -> contributor.authenticatedPatterns();
        };
    }
}
