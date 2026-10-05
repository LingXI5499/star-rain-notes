package com.starrainnotes.tutorial.content.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 知识体系分类实体。表名与列清单由 mapper/tutorial/TutorialCategoryMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
@Data
public class TutorialCategoryEntity {
    private Long id;
    private String name;
    private String slug;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
