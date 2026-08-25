package com.starrainnotes.account.dto;

import java.util.List;

public record AccountSessionView(boolean authenticated, String email, String role, String accountStatus,
                                 List<String> capabilities) {
    public static AccountSessionView anonymous() {
        return new AccountSessionView(false, null, null, null, List.of());
    }
}