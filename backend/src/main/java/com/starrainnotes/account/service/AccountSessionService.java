package com.starrainnotes.account.service;

import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AccountSessionService {
    public long currentAccountId() {
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        Object principal=authentication==null?null:authentication.getPrincipal();
        if(principal instanceof AccountPrincipal account) return account.getId();
        throw new ApiException(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Not authenticated","Account login required.");
    }
}
