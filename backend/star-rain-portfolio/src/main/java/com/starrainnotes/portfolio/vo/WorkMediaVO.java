/*
 * WorkMediaVO：作品模块的响应数据文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.portfolio.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作品媒体响应对象，属于作品模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkMediaVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
    /**
     * 关联的媒体资产标识。
     */
    private String mediaAssetId;
    /**
     * usage类型。
     */
    private String usageType;
    /**
     * 说明。
     */
    private String caption;
    /**
     * 展示排序值。
     */
    private int sortOrder;
    /**
     * 地址。
     */
    private String url;
}
