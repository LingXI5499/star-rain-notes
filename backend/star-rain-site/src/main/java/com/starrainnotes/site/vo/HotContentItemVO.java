/*
 * HotContentItemVO：站点模块的响应数据文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.site.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Hot内容Item响应对象，属于站点模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@AllArgsConstructor
public class HotContentItemVO {
    /**
     * 类型。
     */
    private String type;
    /**
     * 标题。
     */
    private String title;
    /**
     * 路径。
     */
    private String path;
    /**
     * view数量。
     */
    private long viewCount;
}
