package com.starrainnotes.account.vo;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountAuditVO {

    private String id;
    private String actorAccountId;
    private String targetAccountId;
    private String actionCode;
    private String result;
    private String ipAddress;
    private LocalDateTime createdAt;
}
