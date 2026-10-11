/*
 * BlogTopicSummary：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.blog.api.dto;

import java.time.LocalDateTime;

/** Public topic metadata used by SEO without exposing blog persistence types. */
public record BlogTopicSummary(
    /**
     * 当前业务对象的数据库标识。
     */
    Long id,
    /**
     * 公开固定链接的路径标识。
     */
    String slug,
    /**
     * 名称。
     */
    String name,
    /**
     * 描述。
     */
    String description,
                               /**
                                * 更新时间。
                                */
                               LocalDateTime updatedAt) {
}
