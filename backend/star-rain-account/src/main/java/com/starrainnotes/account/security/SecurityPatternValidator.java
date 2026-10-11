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
public final class SecurityPatternValidator {
    private SecurityPatternValidator() {
    }

    public static void validate(List<ModuleSecurityContributor> contributors) {
        for (ModuleSecurityContributor left : contributors) {
            for (ModuleSecurityContributor right : contributors) {
                rejectPublicAgainstAuthenticated(left, right);
                rejectDeadDenied(left, right);
            }
        }
    }

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
    private static void rejectUnlessNarrower(ModuleSecurityContributor deniedOwner, String deniedPattern,
                                             String grantedAccess, ModuleSecurityContributor grantedOwner,
                                             String grantedPattern) {
        if (!overlaps(deniedPattern, grantedPattern)) return;
        boolean deniedIsCarveOut = covers(grantedPattern, deniedPattern) && !covers(deniedPattern, grantedPattern);
        if (!deniedIsCarveOut) {
            throw conflict("denied/" + grantedAccess, deniedOwner, deniedPattern, grantedOwner, grantedPattern);
        }
    }

    private static boolean overlaps(String left, String right) {
        return covers(left, right) || covers(right, left);
    }

    private static IllegalStateException conflict(String kind, ModuleSecurityContributor leftOwner, String left,
                                                  ModuleSecurityContributor rightOwner, String right) {
        return new IllegalStateException("Conflicting " + kind + " security patterns: "
                + leftOwner.moduleName() + " " + left + " and " + rightOwner.moduleName() + " " + right);
    }

    private static boolean covers(String candidate, String other) {
        if (candidate.equals(other)) return true;
        if (candidate.endsWith("/**")) {
            String prefix = candidate.substring(0, candidate.length() - 3);
            return other.equals(prefix) || other.startsWith(prefix + "/");
        }
        return false;
    }
}
