/*
 * HomeSectionPatchDTO：站点模块的请求与传输数据文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.site.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 首页区块Patch传输对象，属于站点模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class HomeSectionPatchDTO {
    /**
     * 显示名称。
     */
    private String displayName;
    /**
     * 已启用。
     */
    private Boolean enabled;
    /**
     * 配置。
     */
    private JsonNode config;
}
