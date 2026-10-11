/*
 * ExperienceEntity：作者资料模块的持久化实体文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

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
/**
 * Experience实体，属于作者资料模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class ExperienceEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 资料标识。
     */
    private Long profileId;
    /**
     * experience类型。
     */
    private String experienceType;
    /**
     * 标题。
     */
    private String title;
    /**
     * organization。
     */
    private String organization;
    /**
     * 开始日期。
     */
    private LocalDate startDate;
    /**
     * 结束日期。
     */
    private LocalDate endDate;
    /**
     * 是否当前。
     */
    private Boolean isCurrent;
    /**
     * 描述Md。
     */
    private String descriptionMd;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
