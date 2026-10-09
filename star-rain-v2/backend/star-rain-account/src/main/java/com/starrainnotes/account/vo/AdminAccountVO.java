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
public class AdminAccountVO {

    private String id;
    private String username;
    private String email;
    private String displayName;
    private String status;
    private Set<String> roles;
    private LocalDateTime createdAt;
}
