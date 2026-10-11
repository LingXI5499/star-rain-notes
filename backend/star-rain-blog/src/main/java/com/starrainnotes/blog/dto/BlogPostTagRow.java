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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostTagRow {

    private Long postId;
    private Long tagId;
    private String slug;
    private String name;
}
