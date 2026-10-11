package com.starrainnotes.account.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EmailVerificationEntity {
    private String email;
    private String codeHash;
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
    private LocalDateTime sentAt;
    private LocalDateTime windowStartedAt;
    private int sendCount;
    private int failedAttempts;
}
