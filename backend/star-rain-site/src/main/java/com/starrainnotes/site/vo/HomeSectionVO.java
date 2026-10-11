/*
 * HomeSectionVO：站点模块的响应数据文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.site.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 首页区块响应对象，属于站点模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@AllArgsConstructor
public class HomeSectionVO {
    /**
     * 代码。
     */
    private String code;
    /**
     * 标题。
     */
    private String title;
    /**
     * data。
     */
    private Object data;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * layout。
     */
    private String layout;
}
