/*
 * HomeSectionEntity：站点模块的持久化实体文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.site.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 首页区块实体，属于站点模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class HomeSectionEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 区块代码。
     */
    private String sectionCode;
    /**
     * 显示名称。
     */
    private String displayName;
    /**
     * 已启用。
     */
    private Boolean enabled;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
    /**
     * 配置JSON。
     */
    private String configJson;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
