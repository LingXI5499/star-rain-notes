package com.starrainnotes.account.security;

import java.io.Serializable;
import java.util.List;

/** Authenticated account principal carrying role for authorization. */
public class AccountPrincipal implements Serializable {

    private final Long id;
    private final String email;
    private final String role;
    private final int authVersion;

    public AccountPrincipal(Long id, String email, String role, int authVersion) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.authVersion = authVersion;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public int getAuthVersion() { return authVersion; }

    public List<String> authorities() {
        return List.of("ROLE_" + role);
    }
}
