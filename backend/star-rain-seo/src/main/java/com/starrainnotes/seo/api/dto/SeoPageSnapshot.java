/*
 * SeoPageSnapshot：公开页面与 SEO模块的请求与传输数据文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.seo.api.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * SEO分页快照，属于公开页面与 SEO模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class SeoPageSnapshot {
    /**
     * 路由路径。
     */
    private String routePath;
    /**
     * 内容类型或请求声明的 Content-Type。
     */
    private String contentType;
    /**
     * 内容标识。
     */
    private Long contentId;
    /**
     * 规范地址。
     */
    private String canonicalUrl;
    /**
     * 标题。
     */
    private String title;
    /**
     * 描述。
     */
    private String description;
    /**
     * robotsDirective。
     */
    private String robotsDirective;
    /**
     * structuredDataJSON。
     */
    private String structuredDataJson;
    /**
     * HTML快照。
     */
    private String htmlSnapshot;
    /**
     * 来源版本Ref。
     */
    private String sourceVersionRef;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * generated时间。
     */
    private LocalDateTime generatedAt;
}
