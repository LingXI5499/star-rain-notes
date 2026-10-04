package com.starrainnotes.tutorial.vo;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlanVO {
    private String id;
    private String tutorialId;
    private String name;
    private String scopeType;
    private String scopeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Integer> studyWeekdays;
    private Integer dailyTargetMinutes;
    private String status;
    private Integer generatedVersion;
}
