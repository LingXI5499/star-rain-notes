/*
 * AnalyticsEventEntity：访问统计模块的持久化实体文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.analytics.entity;

import lombok.Data;

/**
 * 统计事件实体，属于访问统计模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class AnalyticsEventEntity {
    /**
     * 事件类型。
     */
    private String eventType;
    /**
     * 路由键。
     */
    private String routeKey;
    /**
     * 内容类型或请求声明的 Content-Type。
     */
    private String contentType;
    /**
     * 内容标识。
     */
    private Long contentId;
    /**
     * referrer分类。
     */
    private String referrerCategory;
    /**
     * referrer主机。
     */
    private String referrerHost;
}
