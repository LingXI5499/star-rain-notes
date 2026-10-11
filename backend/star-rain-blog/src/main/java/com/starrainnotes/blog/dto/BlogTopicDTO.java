/*
 * BlogTopicDTO：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.blog.dto;

import lombok.Data;

/*
 * Topic 新增与修改共用的请求体。
 * 与 BlogTagDTO 的唯一差异在语义：Topic 的 name 不做唯一校验。
 */
/**
 * 博客专题传输对象，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class BlogTopicDTO {

    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 名称。
     */
    private String name;
    /**
     * 描述。
     */
    private String description;
    /**
     * 精选。
     */
    private Boolean featured;
}
