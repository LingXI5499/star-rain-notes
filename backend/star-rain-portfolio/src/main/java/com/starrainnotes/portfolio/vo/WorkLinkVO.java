/*
 * WorkLinkVO：作品模块的响应数据文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.portfolio.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作品链接响应对象，属于作品模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkLinkVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
    /**
     * 链接类型。
     */
    private String linkType;
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
    private int sortOrder;
    /**
     * 已启用。
     */
    private boolean enabled;
}
