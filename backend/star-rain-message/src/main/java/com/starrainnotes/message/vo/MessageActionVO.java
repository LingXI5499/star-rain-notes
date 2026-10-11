package com.starrainnotes.message.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageActionVO {
    private String actionType;
    private String actorType;
    private LocalDateTime createdAt;
    private String note;
}
