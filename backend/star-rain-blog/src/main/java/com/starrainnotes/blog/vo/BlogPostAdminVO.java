package com.starrainnotes.blog.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 后台文章列表项。
 *
 * 刻意不含正文：列表页只需要判断“有没有正文”，靠 hasBody 就够了，
 * 把整篇 Markdown 塞进分页响应会让后台列表变成最重的接口。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostAdminVO {

    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String status;

    private Long coverMediaAssetId;
    // 由 Media 模块给出，Blog 不自己拼媒体地址
    private String coverUrl;

    private boolean hasBody;

    private LocalDateTime publishedAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<BlogTagVO> tags;
    private List<BlogTopicVO> topics;
}
