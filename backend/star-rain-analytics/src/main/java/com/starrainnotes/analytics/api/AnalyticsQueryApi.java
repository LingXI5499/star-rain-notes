/*
 * AnalyticsQueryApi：访问统计模块的跨模块接口文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：siteSummary（站点摘要处理入口）；trend（trend处理入口）；hotContent（hot内容处理入口）；referrers（referrers处理入口）。
 */

package com.starrainnotes.analytics.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计查询参数接口，属于访问统计模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface AnalyticsQueryApi {
    /**
     * 站点摘要处理入口。
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
    Map<String, Object> siteSummary();
    /**
     * trend处理入口。
     *
     * @param start 开始；类型为 <code>LocalDate</code>
     * @param end 结束；类型为 <code>LocalDate</code>
     * @return 本次处理得到的条目集合
     */
    List<Map<String, Object>> trend(LocalDate start, LocalDate end);
    /**
     * hot内容处理入口。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param start 开始；类型为 <code>LocalDate</code>
     * @param end 结束；类型为 <code>LocalDate</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    List<Map<String, Object>> hotContent(String type, LocalDate start, LocalDate end, int limit);
    /**
     * referrers处理入口。
     *
     * @param start 开始；类型为 <code>LocalDate</code>
     * @param end 结束；类型为 <code>LocalDate</code>
     * @return 本次处理得到的条目集合
     */
    List<Map<String, Object>> referrers(LocalDate start, LocalDate end);
}
