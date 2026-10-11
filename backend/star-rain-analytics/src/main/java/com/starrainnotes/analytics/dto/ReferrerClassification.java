/*
 * ReferrerClassification：访问统计模块的请求与传输数据文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 * 阅读入口：getCategory（读取分类）；getHost（读取主机）。
 */

package com.starrainnotes.analytics.dto;

/**
 * ReferrerClassification，属于访问统计模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
public class ReferrerClassification {
    /**
     * 分类。
     */
    private final String category;
    /**
     * 主机。
     */
    private final String host;

    /**
     * 构造ReferrerClassification，接收本类使用的依赖或初始化参数。
     *
     * @param category 分类；类型为 <code>String</code>
     * @param host 主机；类型为 <code>String</code>
     */
    public ReferrerClassification(String category, String host) {
        this.category = category;
        this.host = host;
    }

    /**
     * 读取分类。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String getCategory() { return category; }
    /**
     * 读取主机。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String getHost() { return host; }
}
