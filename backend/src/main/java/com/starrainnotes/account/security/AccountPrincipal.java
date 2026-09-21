package com.starrainnotes.account.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.List;

/** Authenticated account principal carrying role for authorization. */
@Getter
@RequiredArgsConstructor
public class AccountPrincipal implements Serializable {

    private final Long id;
    private final String email;
    private final String role;
    private final int authVersion;

    public List<String> authorities() {
        return List.of("ROLE_" + role);
    }
}
