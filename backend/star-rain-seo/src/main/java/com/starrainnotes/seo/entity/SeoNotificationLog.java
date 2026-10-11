/*
 * SeoNotificationLog：公开页面与 SEO模块的持久化实体文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.seo.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * SEO通知Log，属于公开页面与 SEO模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class SeoNotificationLog {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 提供方代码。
     */
    private String providerCode;
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
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 尝试数量。
     */
    private Integer attemptCount;
    /**
     * 下一项重试时间。
     */
    private LocalDateTime nextRetryAt;
}
