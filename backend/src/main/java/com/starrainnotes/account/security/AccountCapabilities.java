package com.starrainnotes.account.security;

import java.util.List;

/** Shared capability names for session and account login responses. */
public final class AccountCapabilities {
    private static final List<String> COLLABORATE = List.of(
            "TUTORIAL_COLLABORATE", "BLOG_COLLABORATE", "ENGLISH_COLLABORATE");
    private static final List<String> SUPER = List.of(
            "TUTORIAL_COLLABORATE", "BLOG_COLLABORATE", "ENGLISH_COLLABORATE", "SUPER_ADMIN", "REVIEW");

    private AccountCapabilities() {
    }

    public static List<String> collaborate() {
        return COLLABORATE;
    }

    public static List<String> superAdmin() {
        return SUPER;
    }
}
