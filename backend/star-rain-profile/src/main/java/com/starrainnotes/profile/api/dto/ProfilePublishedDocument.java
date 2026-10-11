/*
 * ProfilePublishedDocument：作者资料模块的请求与传输数据文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.profile.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资料已发布文档，属于作者资料模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePublishedDocument {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 标题。
     */
    private String title;
    /**
     * 摘要。
     */
    private String summary;
    /**
     * Markdown 格式的正文。
     */
    private String bodyMarkdown;
}
