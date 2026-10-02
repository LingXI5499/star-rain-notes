package com.starrainnotes.account.security;

import java.io.Serializable;
import java.util.Set;

public record AccountPrincipal(Long accountId, String username, int authVersion,
                               Set<String> roles, Set<String> permissions) implements Serializable {

    public AccountPrincipal {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }
}

