package com.starrainnotes.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 相邻文章只传导航所需字段，不把正文或后台状态带到公开详情。
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostNeighborVO {

    private String slug;
    private String title;
}
