package com.starrainnotes.blog.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 专题视图。
 *
 * memberCount 统计成员关系（含未发布文章），用于后台确认专题非空；
 * 前台专题入口只展示 ENABLED 的专题，不需要这个计数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogTopicVO {

    private Long id;
    private String slug;
    private String name;
    private String description;
    private String status;
    private Long memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
