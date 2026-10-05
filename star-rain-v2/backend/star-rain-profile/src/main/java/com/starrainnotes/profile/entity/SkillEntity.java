package com.starrainnotes.profile.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作者技能/方向/兴趣实体。表名与列映射由 SkillMapper.xml 承担。
 *
 * description / proficiency 原先带 @TableField(updateStrategy = FieldStrategy.ALWAYS)：
 * 允许被清空（写 NULL）。该语义现由 SkillMapper.xml 的 updateContent 无条件 SET 承担。
 */
@Data
public class SkillEntity {
    private Long id;
    private Long profileId;
    private String category;
    private String name;
    private String description;
    private String proficiency;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
