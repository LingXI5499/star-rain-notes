/*
 * ContentViewEvent：访问统计模块的跨模块接口文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：getContentType（读取内容类型）；getContentId（读取内容标识）；getRouteKey（读取路由键）。
 */

package com.starrainnotes.analytics.api;

/**
 * 内容View事件，属于访问统计模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public class ContentViewEvent {
    /**
     * 内容类型或请求声明的 Content-Type。
     */
    private final String contentType;
    /**
     * 内容标识。
     */
    private final Long contentId;
    /**
     * 路由键。
     */
    private final String routeKey;

    /**
     * 构造ContentViewEvent，接收本类使用的依赖或初始化参数。
     *
     * @param contentType 内容类型或请求声明的 Content-Type；类型为 <code>String</code>
     * @param contentId 内容标识；类型为 <code>Long</code>
     * @param routeKey 路由键；类型为 <code>String</code>
     */
    public ContentViewEvent(String contentType, Long contentId, String routeKey) {
        this.contentType = contentType;
        this.contentId = contentId;
        this.routeKey = routeKey;
    }

    /**
     * 读取内容类型。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String getContentType() { return contentType; }
    /**
     * 读取内容标识。
     * @return 类型为 <code>Long</code> 的处理结果
     */
    public Long getContentId() { return contentId; }
    /**
     * 读取路由键。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String getRouteKey() { return routeKey; }
}
