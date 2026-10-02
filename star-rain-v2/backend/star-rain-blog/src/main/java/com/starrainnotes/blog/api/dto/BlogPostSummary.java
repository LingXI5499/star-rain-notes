package com.starrainnotes.blog.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 模块间文章摘要（Site 首页、Profile 精选内容）。
 *
 * 只包含公开可展示字段，且只对 PUBLISHED 文章生成：
 * 业务模块拿不到 status，也就不会出现“把草稿渲染到首页”这种错误用法。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostSummary {

    private Long id;
    private String slug;
    private String title;
    private String summary;

    private String coverUrl;

    private LocalDateTime publishedAt;

    private List<String> tagNames;
    private List<String> topicNames;
}
