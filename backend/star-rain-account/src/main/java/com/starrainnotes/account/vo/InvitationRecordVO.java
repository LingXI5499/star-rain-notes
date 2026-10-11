package com.starrainnotes.account.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRecordVO {

    private String id;
    private String targetAccountId;
    private String targetEmail;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastSentAt;
    private String mailSubmissionStatus;
}
