package com.starrainnotes.account.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationCodeVO {

    private int expiresInSeconds;
    private int resendAfterSeconds;
    private boolean existingAccount;
}
