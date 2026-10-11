/*
 * SocialLinkEntity：作者资料模块的持久化实体文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.profile.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作者外部社交链接实体。@TableName / @TableId 已移除，映射由 SocialLinkMapper.xml 承担。
 */
/**
 * Social链接实体，属于作者资料模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class SocialLinkEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 资料标识。
     */
    private Long profileId;
    /**
     * platform代码。
     */
    private String platformCode;
    /**
     * 标签。
     */
    private String label;
    /**
     * 地址。
     */
    private String url;
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
