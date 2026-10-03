package com.starrainnotes.blog.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 前台文章详情：在列表项之上补充正文与最后更新时间
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostPublicDetailVO {

    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String bodyMarkdown;

    private Long coverMediaAssetId;
    private String coverUrl;

    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;

    private BlogPostNeighborVO previous;
    private BlogPostNeighborVO next;

    private List<BlogTagVO> tags;
    private List<BlogTopicVO> topics;
}
