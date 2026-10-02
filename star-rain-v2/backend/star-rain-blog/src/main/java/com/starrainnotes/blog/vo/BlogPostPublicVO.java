package com.starrainnotes.blog.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 前台文章列表项。
 *
 * 只暴露公开可展示的字段：没有 status、没有创建者账户 ID、没有正文。
 * 前台不需要知道文章处于哪个内部状态，只知道它已经发布。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostPublicVO {

    private Long id;
    private String slug;
    private String title;
    private String summary;

    private Long coverMediaAssetId;
    private String coverUrl;

    private LocalDateTime publishedAt;

    private List<BlogTagVO> tags;
    private List<BlogTopicVO> topics;
}
