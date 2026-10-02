package com.starrainnotes.account.vo;

public record EmailVerificationCodeVO(int expiresInSeconds, int resendAfterSeconds, boolean existingAccount) {
}
