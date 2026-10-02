package com.starrainnotes.account.context;

import java.io.Serializable;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class AccountPrincipal implements Serializable {

    private Long accountId;
    private String username;
    private int authVersion;
    private Set<String> roles;
    private Set<String> permissions;

    public AccountPrincipal(Long accountId, String username, int authVersion,
                            Set<String> roles, Set<String> permissions) {
        this.accountId = accountId;
        this.username = username;
        this.authVersion = authVersion;
        this.roles = Set.copyOf(roles);
        this.permissions = Set.copyOf(permissions);
    }
}
