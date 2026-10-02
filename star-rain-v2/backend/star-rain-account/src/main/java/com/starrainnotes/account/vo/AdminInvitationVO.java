package com.starrainnotes.account.vo;

import java.time.LocalDateTime;

public record AdminInvitationVO(String id, String targetAccountId,
        LocalDateTime expiresAt, String invitationUrl, String targetEmail, String mailSubmissionStatus) {
}
