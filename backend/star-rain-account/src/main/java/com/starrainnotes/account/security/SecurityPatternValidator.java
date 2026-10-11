/*
 * SecurityPatternValidator：账号模块的安全策略文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 * 主要协作类型：ModuleSecurityContributor。
 * 阅读入口：validate（校验安全模式校验器）。
 */

package com.starrainnotes.account.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;

/*
 * URL 边界的静态校验：同一段地址被两个模块声明成语义不同的访问级别时，**启动即失败**。
 *
 * 校验的是三类两两组合：
 *   1. public × authenticated —— 完全互斥，任何重叠都是矛盾声明。
 *   2. denied × public / denied × authenticated —— 只有一种重叠是合法的：
 *      denied 规则比对方**更窄**，即「在一片放行里挖掉一个洞」。
 *      例如 denied `/api/admin/accounts/* /roles` 挖在 authenticated `/api/admin/accounts/**` 里。
 *      反过来（denied 更宽、或两者相同）会让被覆盖的那条规则变成永不生效的死声明，同样报错。
 *
 * 这里报错而不是静默取优先级，是因为优先级由 SecurityRulePlan 的落地顺序承载：
 * 一旦有人改了那个顺序，校验过的配置也会换一个赢家。启动期拒绝重叠，
 * 把「谁赢」从运行期推断变成配置期事实。
 *
 * 例外说明：denied × denied 的重叠是允许的（多个模块可以封同一段地址）；
 * 纯粹的模式包含关系（authenticated `/a/**` 与 authenticated `/a/b`）也不报错 ——
 * 它们语义相同，前者只是更宽，谁先匹配结果一样。
 */
/**
 * 安全模式校验器，属于账号模块。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 */
public final class SecurityPatternValidator {
    /**
     * 构造SecurityPatternValidator，接收本类使用的依赖或初始化参数。
     */
    private SecurityPatternValidator() {
    }

    /**
     * 校验安全模式校验器。
     * 关键处理步骤调用 <code>rejectPublicAgainstAuthenticated</code>、<code>rejectDeadDenied</code>。
     *
     * @param contributors contributors；类型为 <code>List&lt;ModuleSecurityContributor&gt;</code>
     */
    public static void validate(List<ModuleSecurityContributor> contributors) {
        for (ModuleSecurityContributor left : contributors) {
            for (ModuleSecurityContributor right : contributors) {
                rejectPublicAgainstAuthenticated(left, right);
                rejectDeadDenied(left, right);
            }
        }
    }

    /**
     * 拒绝公开Against已认证。
     * 关键处理步骤调用 <code>publicOwner.publicPatterns</code>、<code>authenticatedOwner.authenticatedPatterns</code>、<code>overlaps</code>、<code>conflict</code>。
     * 边界判断包含 <code>(overlaps(publicPattern, authenticatedPattern))</code>。
     *
     * @param publicOwner 公开Owner；类型为 <code>ModuleSecurityContributor</code>
     * @param authenticatedOwner 已认证Owner；类型为 <code>ModuleSecurityContributor</code>
     */
    private static void rejectPublicAgainstAuthenticated(ModuleSecurityContributor publicOwner,
                                                         ModuleSecurityContributor authenticatedOwner) {
        for (String publicPattern : publicOwner.publicPatterns()) {
            for (String authenticatedPattern : authenticatedOwner.authenticatedPatterns()) {
                if (overlaps(publicPattern, authenticatedPattern)) {
                    throw conflict("public/authenticated", publicOwner, publicPattern,
                            authenticatedOwner, authenticatedPattern);
                }
            }
        }
    }

    /**
     * 拒绝Dead被拒绝。
     * 关键处理步骤调用 <code>deniedOwner.deniedPatterns</code>、<code>grantedOwner.publicPatterns</code>、<code>rejectUnlessNarrower</code>、<code>grantedOwner.authenticatedPatterns</code>。
     *
     * @param deniedOwner 被拒绝Owner；类型为 <code>ModuleSecurityContributor</code>
     * @param grantedOwner 已授权Owner；类型为 <code>ModuleSecurityContributor</code>
     */
    private static void rejectDeadDenied(ModuleSecurityContributor deniedOwner,
                                         ModuleSecurityContributor grantedOwner) {
        for (String deniedPattern : deniedOwner.deniedPatterns()) {
            for (String grantedPattern : grantedOwner.publicPatterns()) {
                rejectUnlessNarrower(deniedOwner, deniedPattern, "public", grantedOwner, grantedPattern);
            }
            for (String grantedPattern : grantedOwner.authenticatedPatterns()) {
                rejectUnlessNarrower(deniedOwner, deniedPattern, "authenticated", grantedOwner, grantedPattern);
            }
        }
    }

    /*
     * denied 与一条放行规则重叠时，唯一合法的形态是 denied 严格更窄：
     * 放行规则覆盖 denied（说明 denied 落在它内部），且 denied 不覆盖放行规则（说明不是同宽或更宽）。
     */
    /**
     * 拒绝UnlessNarrower。
     * 关键处理步骤调用 <code>overlaps</code>、<code>covers</code>、<code>conflict</code>。
     * 边界判断包含 <code>(!overlaps(deniedPattern, grantedPattern))</code>；<code>(!deniedIsCarveOut)</code>。
     *
     * @param deniedOwner 被拒绝Owner；类型为 <code>ModuleSecurityContributor</code>
     * @param deniedPattern 被拒绝模式；类型为 <code>String</code>
     * @param grantedAccess 已授权访问；类型为 <code>String</code>
     * @param grantedOwner 已授权Owner；类型为 <code>ModuleSecurityContributor</code>
     * @param grantedPattern 已授权模式；类型为 <code>String</code>
     */
    private static void rejectUnlessNarrower(ModuleSecurityContributor deniedOwner, String deniedPattern,
                                             String grantedAccess, ModuleSecurityContributor grantedOwner,
                                             String grantedPattern) {
        if (!overlaps(deniedPattern, grantedPattern)) return;
        boolean deniedIsCarveOut = covers(grantedPattern, deniedPattern) && !covers(deniedPattern, grantedPattern);
        if (!deniedIsCarveOut) {
            throw conflict("denied/" + grantedAccess, deniedOwner, deniedPattern, grantedOwner, grantedPattern);
        }
    }

    /**
     * overlaps处理入口。
     * 关键处理步骤调用 <code>covers</code>。
     *
     * @param left left；类型为 <code>String</code>
     * @param right right；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    private static boolean overlaps(String left, String right) {
        return covers(left, right) || covers(right, left);
    }

    /**
     * 冲突处理入口。
     * 关键处理步骤调用 <code>leftOwner.moduleName</code>、<code>rightOwner.moduleName</code>。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param leftOwner leftOwner；类型为 <code>ModuleSecurityContributor</code>
     * @param left left；类型为 <code>String</code>
     * @param rightOwner rightOwner；类型为 <code>ModuleSecurityContributor</code>
     * @param right right；类型为 <code>String</code>
     * @return 类型为 <code>IllegalStateException</code> 的处理结果
     */
    private static IllegalStateException conflict(String kind, ModuleSecurityContributor leftOwner, String left,
                                                  ModuleSecurityContributor rightOwner, String right) {
        return new IllegalStateException("Conflicting " + kind + " security patterns: "
                + leftOwner.moduleName() + " " + left + " and " + rightOwner.moduleName() + " " + right);
    }

    /**
     * covers处理入口。
     * 关键处理步骤调用 <code>candidate.equals</code>、<code>candidate.endsWith</code>、<code>candidate.substring</code>、<code>candidate.length</code>。
     * 边界判断包含 <code>(candidate.equals(other))</code>；<code>(candidate.endsWith("/**"))</code>。
     *
     * @param candidate candidate；类型为 <code>String</code>
     * @param other other；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    private static boolean covers(String candidate, String other) {
        if (candidate.equals(other)) return true;
        if (candidate.endsWith("/**")) {
            String prefix = candidate.substring(0, candidate.length() - 3);
            return other.equals(prefix) || other.startsWith(prefix + "/");
        }
        return false;
    }
}
