/*
 * BlogPostTagRow：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 文章标签投影行（层间传输对象）。
 *
 * 批量查询多篇文章的标签时使用：一次 IN 查询拿回全部行，在 Service 里按 postId 分组，
 * 避免列表接口为每篇文章单独查一次标签（N+1）。
 */
/**
 * 博客文章标签行，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostTagRow {

    /**
     * 文章标识。
     */
    private Long postId;
    /**
     * 标签标识。
     */
    private Long tagId;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 名称。
     */
    private String name;
}
