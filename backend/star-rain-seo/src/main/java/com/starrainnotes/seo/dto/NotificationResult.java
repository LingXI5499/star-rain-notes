package com.starrainnotes.seo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResult {
    private boolean success;
    private Integer httpStatus;
    private String errorCode;
}
