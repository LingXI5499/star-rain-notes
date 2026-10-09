package com.starrainnotes.account.security;

import com.starrainnotes.account.context.AccountPrincipal;
import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.mapper.AccountMapper;
import java.util.ArrayList;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AccountAuthenticator implements AuthenticationProvider {

    private final AccountMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public AccountAuthenticator(AccountMapper mapper, PasswordEncoder passwordEncoder) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String identifier = String.valueOf(authentication.getPrincipal()).trim();
        String password = String.valueOf(authentication.getCredentials());
        AccountEntity account = mapper.accountByIdentifier(identifier);
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw new BadCredentialsException("身份信息无效");
        }
        String hash = mapper.passwordHash(account.getId());
        if (hash == null || !passwordEncoder.matches(password, hash)) {
            throw new BadCredentialsException("身份信息无效");
        }
        Set<String> roles = Set.copyOf(mapper.roleCodes(account.getId()));
        Set<String> permissions = Set.copyOf(mapper.permissionCodes(account.getId()));
        AccountPrincipal principal = new AccountPrincipal(
                account.getId(), account.getUsername(), account.getAuthVersion(), roles, permissions);
        ArrayList<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

