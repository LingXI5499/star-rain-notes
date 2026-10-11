/*
 * BlogPostQueryDTO：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.blog.dto;

import lombok.Data;

/*
 * 后台文章列表查询条件。
 *
 * 查询接口不写校验注解：page / pageSize / status 的合法性由 Service 显式校验，
 * 越界分页返回 BLOG_QUERY_INVALID，而不是让数据库报错后变成 500。
 */
/**
 * 博客文章查询参数传输对象，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class BlogPostQueryDTO {

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

    // 标题与 slug 模糊匹配
    /**
     * 关键词。
     */
    private String keyword;

    // DRAFT / PUBLISHED / WITHDRAWN，空白表示全部
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;

    /**
     * 标签标识。
     */
    private Long tagId;
    /**
     * 专题标识。
     */
    private Long topicId;
}
