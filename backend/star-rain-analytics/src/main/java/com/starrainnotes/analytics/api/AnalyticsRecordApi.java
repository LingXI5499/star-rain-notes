/*
 * AnalyticsRecordApi：访问统计模块的跨模块接口文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：recordContentView（记录内容View）。
 */

package com.starrainnotes.analytics.api;

/**
 * 统计记录接口，属于访问统计模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface AnalyticsRecordApi {
    /**
     * 记录内容View。
     *
     * @param event 事件；类型为 <code>ContentViewEvent</code>
     */
    void recordContentView(ContentViewEvent event);
}
