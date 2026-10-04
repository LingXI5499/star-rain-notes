package com.starrainnotes.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_profile_experience")
public class ExperienceEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long profileId;
    private String experienceType;
    private String title;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String organization;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private LocalDate startDate;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private LocalDate endDate;
    private Boolean isCurrent;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String descriptionMd;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
