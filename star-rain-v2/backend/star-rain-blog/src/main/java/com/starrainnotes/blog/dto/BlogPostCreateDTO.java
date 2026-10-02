package com.starrainnotes.blog.dto;

import java.util.List;
import lombok.Data;

/*
 * BLOG-003 创建文章请求。
 *
 * 只放元数据与标签绑定，正文单独走 PUT /body：
 * 正文可能很大，合并进一个 DTO 会让每次改标题都重传整篇文章。
 * 这里刻意不加 jakarta.validation 注解，校验统一在 Service 里做，
 * 错误码才能稳定为 BLOG_* 而不是笼统的 INVALID_REQUEST。
 */
@Data
public class BlogPostCreateDTO {

    private String title;
    private String slug;
    private String summary;

    // 没有封面时留空
    private Long coverMediaAssetId;

    // 允许为空：先建草稿，之后再补标签
    private List<Long> tagIds;
}
