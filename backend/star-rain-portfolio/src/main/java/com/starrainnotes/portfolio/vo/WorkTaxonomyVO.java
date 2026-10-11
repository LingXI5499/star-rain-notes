/*
 * WorkTaxonomyVO：作品模块的响应数据文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.portfolio.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/**
 * 作品Taxonomy响应对象，属于作品模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkTaxonomyVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
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
}
