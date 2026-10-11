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
public class AdminInvitationVO {

    private String id;
    private String targetAccountId;
    private LocalDateTime expiresAt;
    private String invitationUrl;
    private String targetEmail;
    private String mailSubmissionStatus;
}
