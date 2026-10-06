package com.starrainnotes.tutorial.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/* 教程概览的聚合查询结果，不作为 HTTP 响应直接返回。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialDashboardStatsDTO {
    private long total;
    private long drafts;
    private long chapters;
    private long draftChapters;
}
