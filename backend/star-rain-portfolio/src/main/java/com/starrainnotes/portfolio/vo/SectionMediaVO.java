/*
 * SectionMediaVO：作品模块的响应数据文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.portfolio.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/**
 * 区块媒体响应对象，属于作品模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SectionMediaVO {
    /**
     * 关联的媒体资产标识。
     */
    private String mediaAssetId;
    /**
     * 说明。
     */
    private String caption;
    /**
     * 地址。
     */
    private String url;
    /**
     * 媒体类型。
     */
    private String mediaType;
}
