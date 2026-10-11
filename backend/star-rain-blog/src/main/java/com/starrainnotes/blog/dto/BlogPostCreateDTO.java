/*
 * BlogPostCreateDTO：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.blog.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * BLOG-003 创建文章请求。
 *
 * 只放元数据与标签绑定，正文单独走 PUT /body：
 * 正文可能很大，合并进一个 DTO 会让每次改标题都重传整篇文章。
 * 这里刻意不加 jakarta.validation 注解，校验统一在 Service 里做，
 * 错误码才能稳定为 BLOG_* 而不是笼统的 INVALID_REQUEST。
 */
/**
 * 博客文章创建传输对象，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostCreateDTO {

    /**
     * 标题。
     */
    private String title;
    // 可选：不填时从标题派生，中文标题使用确定性回退值。
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 摘要。
     */
    private String summary;

    // 没有封面时留空
    /**
     * 封面媒体资产标识。
     */
    private Long coverMediaAssetId;

    // 允许为空：先建草稿，之后再补标签
    /**
     * 标签标识集合。
     */
    private List<Long> tagIds;
}
