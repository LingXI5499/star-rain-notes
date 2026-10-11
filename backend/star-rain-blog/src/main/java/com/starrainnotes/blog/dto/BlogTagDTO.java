/*
 * BlogTagDTO：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.blog.dto;

import lombok.Data;

/*
 * Tag 新增与修改共用的请求体。
 *
 * 修改时 null 表示保持原值，因此不能靠置 null 清空 name / slug（它们本身非空）。
 * description 传空字符串即清空说明。
 */
/**
 * 博客标签传输对象，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class BlogTagDTO {

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
}
