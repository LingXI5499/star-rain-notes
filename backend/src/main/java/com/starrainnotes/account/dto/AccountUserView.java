package com.starrainnotes.account.dto;

import com.starrainnotes.account.entity.AccountUser;

import java.time.LocalDateTime;

public record AccountUserView(
        Long id,
        String email,
        String role,
        String accountStatus,
        LocalDateTime emailVerifiedAt,
        LocalDateTime activatedAt,
        LocalDateTime lockedUntil,
        LocalDateTime lastLoginAt,
        LocalDateTime passwordChangedAt,
        LocalDateTime disabledAt,
        Long disabledBy,
        String disabledReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AccountUserView from(AccountUser user) {
        return new AccountUserView(user.getId(), user.getEmail(), user.getRole(), user.getAccountStatus(),
                user.getEmailVerifiedAt(), user.getActivatedAt(), user.getLockedUntil(), user.getLastLoginAt(),
                user.getPasswordChangedAt(), user.getDisabledAt(), user.getDisabledBy(), user.getDisabledReason(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
