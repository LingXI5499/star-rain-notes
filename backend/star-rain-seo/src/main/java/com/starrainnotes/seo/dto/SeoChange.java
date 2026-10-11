/*
 * SeoChange：公开页面与 SEO模块的请求与传输数据文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.seo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SEOChange，属于公开页面与 SEO模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeoChange {
    /**
     * 路由路径。
     */
    private String routePath;
    /**
     * 规范地址。
     */
    private String canonicalUrl;
    /**
     * change类型。
     */
    private String changeType;
}
