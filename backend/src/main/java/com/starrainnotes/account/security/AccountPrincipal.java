package com.starrainnotes.account.security;

import java.io.Serializable;
import java.util.List;

/** Authenticated account principal carrying role for authorization. */
public class AccountPrincipal implements Serializable {

    private final Long id;
    private final String email;
    private final String role;

    public AccountPrincipal(Long id, String email, String role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    public List<String> authorities() {
        return List.of("ROLE_" + role);
    }
}