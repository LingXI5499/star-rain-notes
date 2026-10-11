/*
 * WorkTaxonomyEntity：作品模块的持久化实体文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.portfolio.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/**
 * 作品Taxonomy实体，属于作品模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkTaxonomyEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 代码。
     */
    private String code;
    /**
     * 名称。
     */
    private String name;
    /**
     * 分组代码。
     */
    private String groupCode;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
}
