/*
 * PageViewDTO：访问统计模块的请求与传输数据文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.analytics.dto;

import lombok.Data;

/**
 * 分页View传输对象，属于访问统计模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class PageViewDTO {
    /**
     * 路由键。
     */
    private String routeKey;
    /**
     * referrer。
     */
    private String referrer;
}
