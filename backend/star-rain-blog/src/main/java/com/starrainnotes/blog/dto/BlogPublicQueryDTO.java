/*
 * BlogPublicQueryDTO：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.blog.dto;

import lombok.Data;

/*
 * BLOG-001 / BLOG-010 公开查询条件：列表与归档共用一个 DTO。
 *
 * year / month 只对归档有意义，列表接口忽略它们；
 * 共用一个 DTO 是为了让“组合筛选”的合法性校验只有一处实现。
 */
/**
 * 博客公开查询参数传输对象，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class BlogPublicQueryDTO {

    /**
     * 分页页码。
     * 初始值为 <code>1</code>。
     */
    private int page = 1;
    /**
     * 单页返回条目数。
     * 初始值为 <code>20</code>。
     */
    private int pageSize = 20;

    // Tag slug 与 Topic slug，都走 slug 而不是 ID：公开 URL 不暴露内部主键
    /**
     * 标签。
     */
    private String tag;
    /*
     * 多标签筛选：逗号分隔，「命中任一」（`?tags=java,spring-boot`）。
     * 与单标签参数 tag 是同一件事的两种写法：前端单标签写 tag（链接短、好分享），
     * 多标签写 tags；两个都给时合并去重，不报错。
     */
    /**
     * 标签列表。
     */
    private String tags;
    /**
     * 专题。
     */
    private String topic;

    // 归档时间维度，依据 publishedAt 而不是 createdAt
    /**
     * 年份。
     */
    private Integer year;
    /**
     * 月份。
     */
    private Integer month;
    /**
     * 日。
     */
    private Integer day;
}
