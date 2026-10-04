package com.starrainnotes.message.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminMessageVO {
    private Long id;
    private String authorDisplayName;
    private String contactEmail;
    private String content;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime moderatedAt;
    private String rejectReason;
}
