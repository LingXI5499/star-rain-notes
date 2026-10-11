/*
 * BlogPostBodyDTO：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.blog.dto;

import lombok.Data;

/*
 * BLOG-003 单独更新正文。
 *
 * 正文里的媒体引用由 Service 从 Markdown 里的公开内容地址解析，
 * 不额外收一份 id 列表：两份真源一定会漂移，谁对谁错无法判断。
 */
/**
 * 博客文章正文传输对象，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class BlogPostBodyDTO {

    /**
     * Markdown 格式的正文。
     */
    private String bodyMarkdown;
}
