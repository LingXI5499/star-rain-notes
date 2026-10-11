/*
 * SiteMediaRequestDTO：站点模块的请求与传输数据文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.site.dto;

import lombok.Data;

/**
 * 站点媒体请求传输对象，属于站点模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class SiteMediaRequestDTO {
    /**
     * 关联的媒体资产标识。
     */
    private Long mediaAssetId;
}
