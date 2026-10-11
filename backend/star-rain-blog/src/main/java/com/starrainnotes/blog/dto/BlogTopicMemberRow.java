package com.starrainnotes.blog.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 专题成员行：后台专题排序界面用。
 *
 * 带上文章状态与发布时间，是因为草稿也可能先被排进专题，
 * 界面上必须能看出“这条还没发布”，否则排序结果与前台看到的不一致时无从排查。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogTopicMemberRow {

    private Long postId;
    private String slug;
    private String title;
    private String status;
    private Integer sortOrder;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
