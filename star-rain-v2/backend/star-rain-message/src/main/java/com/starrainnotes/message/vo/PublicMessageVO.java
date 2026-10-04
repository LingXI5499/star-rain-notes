package com.starrainnotes.message.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PublicMessageVO {
    private Long id;
    private String authorDisplayName;
    private String content;
    private LocalDateTime submittedAt;
}
