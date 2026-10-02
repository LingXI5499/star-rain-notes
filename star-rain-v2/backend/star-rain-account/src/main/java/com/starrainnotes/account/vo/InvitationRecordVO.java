package com.starrainnotes.account.vo;

import java.time.LocalDateTime;

public record InvitationRecordVO(String id, String targetAccountId, String targetEmail,
        String status, LocalDateTime createdAt, LocalDateTime expiresAt,
        LocalDateTime lastSentAt, String mailSubmissionStatus) {
}
