package com.starrainnotes.tutorial.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sr_study_plan")
public class StudyPlanEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private Long tutorialId;
    private String name;
    private String scopeType;
    private Long scopeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String studyWeekdaysJson;
    private Integer dailyTargetMinutes;
    private String status;
    private Integer generatedVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
