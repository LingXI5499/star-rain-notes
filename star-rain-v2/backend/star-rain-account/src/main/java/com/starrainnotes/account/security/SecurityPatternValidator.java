package com.starrainnotes.account.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;

/** Fails fast when a public rule and an authenticated rule cover the same URL tree. */
public final class SecurityPatternValidator {
    private SecurityPatternValidator() {
    }

    public static void validate(List<ModuleSecurityContributor> contributors) {
        for (ModuleSecurityContributor publicOwner : contributors) {
            for (String publicPattern : publicOwner.publicPatterns()) {
                for (ModuleSecurityContributor authenticatedOwner : contributors) {
                    for (String authenticatedPattern : authenticatedOwner.authenticatedPatterns()) {
                        if (covers(publicPattern, authenticatedPattern)
                            || covers(authenticatedPattern, publicPattern)) {
                            throw new IllegalStateException("Conflicting public/authenticated security patterns: "
                                + publicOwner.moduleName() + " " + publicPattern + " and "
                                + authenticatedOwner.moduleName() + " " + authenticatedPattern);
                        }
                    }
                }
            }
        }
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
