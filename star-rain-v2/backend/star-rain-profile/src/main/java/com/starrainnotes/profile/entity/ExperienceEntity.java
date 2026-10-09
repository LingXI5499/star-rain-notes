package com.starrainnotes.profile.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作者教育/经历实体。表名与列映射由 ExperienceMapper.xml 承担。
 *
 * organization / startDate / endDate / descriptionMd 原先带
 * @TableField(updateStrategy = FieldStrategy.ALWAYS)：允许被清空（写 NULL）。
 * 该语义现由 ExperienceMapper.xml 的 updateContent 无条件 SET 承担。
 */
@Data
public class ExperienceEntity {
    private Long id;
    private Long profileId;
    private String experienceType;
    private String title;
    private String organization;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
    private String descriptionMd;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
