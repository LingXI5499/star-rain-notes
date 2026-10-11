package com.starrainnotes.message.dto;

import lombok.Data;

@Data
public class MessageSubmitDTO {
    private String authorDisplayName;
    private String contactEmail;
    private String content;
}
