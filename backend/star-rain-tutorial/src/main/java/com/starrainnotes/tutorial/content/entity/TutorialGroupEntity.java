package com.starrainnotes.tutorial.content.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 教程分组实体。表名与列清单由 mapper/tutorial/TutorialGroupMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
@Data
public class TutorialGroupEntity {
    private Long id;
    private Long tutorialId;
    private String title;
    private String description;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
