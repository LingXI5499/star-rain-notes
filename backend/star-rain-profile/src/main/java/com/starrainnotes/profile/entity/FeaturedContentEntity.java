/*
 * FeaturedContentEntity：作者资料模块的持久化实体文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.profile.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作者精选内容引用实体。表名与列映射由 FeaturedContentMapper.xml 承担。
 *
 * titleOverride 原先带 @TableField(updateStrategy = FieldStrategy.ALWAYS)，但这条路径上
 * 没有「编辑精选」的业务，只有新增与排序；新增时 null 写入 NULL 与列默认值一致。
 */
/**
 * 精选内容实体，属于作者资料模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class FeaturedContentEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 资料标识。
     */
    private Long profileId;
    /**
     * 内容类型或请求声明的 Content-Type。
     */
    private String contentType;
    /**
     * 内容标识。
     */
    private Long contentId;
    /**
     * 标题Override。
     */
    private String titleOverride;
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
