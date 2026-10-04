package com.starrainnotes.common.dashboard.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardRecentItem {
    private String id;
    private String type;
    private String title;
    private String status;
    private LocalDateTime updatedAt;
}
